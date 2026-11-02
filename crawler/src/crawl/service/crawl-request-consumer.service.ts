import { setTimeout as sleep } from 'node:timers/promises';
import { type BeforeApplicationShutdown, Injectable, Logger, type OnApplicationBootstrap } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Semaphore } from 'async-mutex';
import type { RuntimeConfig } from '../../config/config.js';
import { Policy } from '../../config/policy.js';
import { type EnvironmentResource, EnvironmentResources } from '../environment-resources.js';
import { CrawlRequestMessage } from '../dto/crawl-request-message.dto.js';
import { CrawlService } from './crawl.service.js';
import type { ReceivedMessage } from '../interfaces/queue.interface.js';

/**
 * 환경마다 요청 큐를 폴링해서 받은 크롤링 요청을 실행하고, 결과를 그 환경의 CrawlQueueBatch 에 넘김
 * 동시에 실행하는 크롤링 수는 모든 환경을 합쳐 concurrency 설정값으로 제한함(세마포어)
 *
 * 메시지를 받기 전에는 세마포어를 획득(acquire)하지 않음. 메시지가 없는 큐가 폴링 대기 중에 세마포어를 쥐고 있으면 다른 환경의 크롤링이 그만큼 밀리기 때문
 * 그래서 두 환경이 세마포어를 획득할 수 있다고 보고 동시에 메시지를 받을 수 있고, 이때 세마포어를 획득하지 못한 메시지는 다른 크롤링이 세마포어를 해제(release)할 때까지 기다림
 * 기다리는 시간은 크롤링 한두 건 정도라 메시지 visibility 시간 안에 끝남
 */
@Injectable()
export class CrawlRequestConsumer implements OnApplicationBootstrap, BeforeApplicationShutdown {
  private readonly logger = new Logger(CrawlRequestConsumer.name);
  private readonly crawlConcurrency: number;
  private readonly crawlConcurrencyLimit: Semaphore;
  private receiveLoops: Promise<unknown> = Promise.resolve();
  private isShuttingDown = false;

  constructor(
    private readonly environments: EnvironmentResources,
    configService: ConfigService<RuntimeConfig, true>,
    private readonly crawlService: CrawlService
  ) {
    this.crawlConcurrency = configService.get('concurrency', { infer: true });
    this.crawlConcurrencyLimit = new Semaphore(this.crawlConcurrency);
  }

  onApplicationBootstrap(): void {
    this.receiveLoops = Promise.all(
      this.environments.all.map((environment) => this.runMessageReceiveLoop(environment))
    );
  }

  private async runMessageReceiveLoop(environment: EnvironmentResource): Promise<void> {
    while (!this.isShuttingDown) {
      await this.crawlConcurrencyLimit.waitForUnlock();
      const receivableMessageCount = this.countReceivableMessages();
      // waitForUnlock 은 세마포어를 획득하지 않고 기다리기만 해서, 그사이 다른 환경이 남은 세마포어를 먼저 획득하면 0 이 됨
      // 밑에 await 에서 양보되면 위에 await 에서 기다리던 dev나 prod중 하나가 내려오면서 receivableMessageCount 가 1이 될 수 있음
      if (receivableMessageCount === 0) {
        continue;
      }

      let receivedMessages: ReceivedMessage[];
      try {
        receivedMessages = await environment.requestQueue.receive(receivableMessageCount);
      } catch (error) {
        this.logger.error(`[${environment.deploymentEnvironment}] 크롤링 요청 수신 실패`, error);
        await sleep(Policy.QUEUE_RETRY_DELAY_MILLIS);
        continue;
      }

      // 중지 중에 받은 메시지는 처리하지 않음. visibility 시간이 지나면 다시 전달됨
      if (this.isShuttingDown) {
        return;
      }

      for (const receivedMessage of receivedMessages) {
        await this.startProcessingWhenSlotFree(environment, receivedMessage);
      }
    }
  }

  /**
   * 지금 바로 크롤링을 시작할 수 있는 수만큼만 큐 메시지를 받음
   *
   * 받은 메시지는 visibility 시간이 지나도록 완료 처리하지 않으면 큐에 다시 나타남. 이미 받아서 기다리는 중이어도 마찬가지임
   * 그래서 한 번에 많이 받아두면 뒤쪽 메시지가 기다리는 사이 큐에 다시 나타나서, 같은 요청을 또 크롤링하게 됨
   * QUEUE_BATCH_SIZE 는 OCI Queue 가 한 번에 받을 수 있는 최대 메시지 수
   */
  private countReceivableMessages(): number {
    return Math.min(this.crawlConcurrencyLimit.getValue(), Policy.QUEUE_BATCH_SIZE);
  }

  /** 세마포어를 획득할 때까지 기다린 뒤 크롤링을 시작함. 수신 루프가 다음 메시지로 넘어갈 수 있게 크롤링이 끝날 때까지는 기다리지 않음 */
  private async startProcessingWhenSlotFree(
    environmentResource: EnvironmentResource,
    receivedMessage: ReceivedMessage
  ): Promise<void> {
    const [, releaseLimit] = await this.crawlConcurrencyLimit.acquire();
    void this.crawlAndBatchResult(environmentResource, receivedMessage).finally(releaseLimit);
  }

  /** 요청 메시지 한 건을 크롤링하고 결과를 CrawlQueueBatch 에 넘김. 실패하면 로그만 남기고 완료 처리하지 않으므로 큐가 다시 전달함 */
  private async crawlAndBatchResult(environment: EnvironmentResource, receivedMessage: ReceivedMessage): Promise<void> {
    const crawlRequest = CrawlRequestMessage.from(receivedMessage.content);

    if (crawlRequest === undefined) {
      this.logger.error(
        `[${environment.deploymentEnvironment}] 형식이 잘못된 크롤링 요청. dead letter queue 로 이동할 때까지 완료 처리하지 않음`,
        { content: receivedMessage.content, deliveryCount: receivedMessage.deliveryCount }
      );
      return;
    }

    try {
      const crawlResult = await this.crawlService.crawl(crawlRequest, environment.bucketName);
      environment.crawlQueueBatch.add(JSON.stringify(crawlResult), receivedMessage.receipt);
    } catch (error) {
      this.logger.error(`[${environment.deploymentEnvironment}] 크롤링 실패. 재전달 후 다시 처리함`, {
        towerId: crawlRequest.towerId,
        scheduledAt: crawlRequest.scheduledAt,
        deliveryCount: receivedMessage.deliveryCount,
        error
      });
    }
  }

  /** 새 메시지를 받지 않고, 처리 중인 크롤링과 모아둔 결과 발행이 끝날 때까지 기다림 */
  async beforeApplicationShutdown(): Promise<void> {
    this.isShuttingDown = true;
    await this.receiveLoops;
    await this.crawlConcurrencyLimit.waitForUnlock(this.crawlConcurrency);
    await this.publishRemainingResults();
  }

  private async publishRemainingResults(): Promise<void> {
    await Promise.all(this.environments.all.map((environment) => environment.crawlQueueBatch.close()));
  }
}
