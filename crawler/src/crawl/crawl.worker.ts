import { setTimeout as sleep } from 'node:timers/promises';
import { type BeforeApplicationShutdown, Injectable, Logger, type OnApplicationBootstrap } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Semaphore } from 'async-mutex';
import type { RuntimeConfig } from '../config/config.js';
import { Policy } from '../config/policy.js';
import { type EnvironmentResources, EnvironmentResourcesList } from './environment-resources.model.js';
import { CrawlRequestMessage } from './dto/crawl-request-message.dto.js';
import { CrawlService } from './service/crawl.service.js';
import type { ReceivedMessage } from './interfaces/queue.interface.js';
import { CrawlResultBatchProcessor } from './crawl-result-batch.processor.js';

/**
 * 환경마다 요청 큐 수신 루프를 하나씩 돌리고, 동시 처리 한도는 모든 환경이 함께 씀
 *
 * 받을 때는 자리를 미리 잡지 않음. 메시지가 없는 큐가 폴링하며 자리를 쥐고 있으면 다른 환경이 그만큼 처리하지 못하기 때문
 * 대신 두 루프가 같은 빈 자리를 보고 동시에 받을 수 있어서, 받은 메시지가 자리를 잠깐 기다릴 수 있음
 * 기다리는 시간은 크롤링 한두 건 수준이라 메시지 visibility 시간 안에 끝남
 */
@Injectable()
export class CrawlWorker implements OnApplicationBootstrap, BeforeApplicationShutdown {
  private readonly logger = new Logger(CrawlWorker.name);
  private readonly crawlConcurrencyLimit: Semaphore;
  private readonly resultBatchProcessorByEnvironment: Map<EnvironmentResources, CrawlResultBatchProcessor>;
  private readonly runningCrawls = new Set<Promise<void>>();
  private receiveLoops: Promise<unknown> = Promise.resolve();
  private isShuttingDown = false;

  constructor(
    private readonly environments: EnvironmentResourcesList,
    configService: ConfigService<RuntimeConfig, true>,
    private readonly crawlService: CrawlService
  ) {
    this.crawlConcurrencyLimit = new Semaphore(configService.get('concurrency', { infer: true }));
    this.resultBatchProcessorByEnvironment = new Map(
      environments.all.map((environment) => {
        return [environment, new CrawlResultBatchProcessor(environment.requestQueue, environment.resultQueue)];
      })
    );
  }

  onApplicationBootstrap(): void {
    this.receiveLoops = Promise.all(
      this.environments.all.map((environment) => this.runMessageReceiveLoop(environment))
    );
  }

  private async runMessageReceiveLoop(environment: EnvironmentResources): Promise<void> {
    while (!this.isShuttingDown) {
      await this.crawlConcurrencyLimit.waitForUnlock();

      let receivedMessages: ReceivedMessage[];
      try {
        receivedMessages = await environment.requestQueue.receive(this.countReceivableMessages());
      } catch (error) {
        this.logger.error(`[${environment.name}] 크롤링 요청 수신 실패`, error);
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

  private countReceivableMessages(): number {
    return Math.min(Math.max(this.crawlConcurrencyLimit.getValue(), 1), Policy.QUEUE_BATCH_SIZE);
  }

  /** 자리가 날 때까지 기다린 뒤 처리를 시작하고, 끝날 때까지 기다리지는 않음 */
  private async startProcessingWhenSlotFree(
    environment: EnvironmentResources,
    receivedMessage: ReceivedMessage
  ): Promise<void> {
    const [, releaseSlot] = await this.crawlConcurrencyLimit.acquire();
    const runningCrawl = this.processMessage(environment, receivedMessage).finally(() => {
      releaseSlot();
      this.runningCrawls.delete(runningCrawl);
    });
    this.runningCrawls.add(runningCrawl);
  }

  /** 예외를 밖으로 던지지 않음. 완료 처리하지 않은 메시지는 큐가 다시 전달함 */
  private async processMessage(environment: EnvironmentResources, receivedMessage: ReceivedMessage): Promise<void> {
    const crawlRequest = CrawlRequestMessage.from(receivedMessage.content);
    if (crawlRequest === undefined) {
      this.logger.error(
        `[${environment.name}] 형식이 잘못된 크롤링 요청. dead letter queue 로 이동할 때까지 완료 처리하지 않음`,
        { content: receivedMessage.content, deliveryCount: receivedMessage.deliveryCount }
      );
      return;
    }

    try {
      const crawlResult = await this.crawlService.crawl(crawlRequest, environment.bucketName);
      this.resultBatchProcessorByEnvironment
        .get(environment)
        ?.add(JSON.stringify(crawlResult), receivedMessage.receipt);
    } catch (error) {
      this.logger.error(`[${environment.name}] 크롤링 실패. 재전달 후 다시 처리함`, {
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
    await Promise.allSettled(this.runningCrawls);
    await this.publishRemainingResults();
  }

  private async publishRemainingResults(): Promise<void> {
    await Promise.all(
      [...this.resultBatchProcessorByEnvironment.values()].map((resultBatchProcessor) => resultBatchProcessor.close())
    );
  }
}
