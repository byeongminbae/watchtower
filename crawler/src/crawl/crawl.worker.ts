import { setTimeout as sleep } from 'node:timers/promises';
import { type BeforeApplicationShutdown, Injectable, Logger, type OnApplicationBootstrap } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import type { CrawlerConfig } from '../config/crawler-config.js';
import { ConcurrencyLimiter } from './concurrency-limiter.js';
import { type CrawlEnvironment, CrawlEnvironments } from './crawl-environment.js';
import { CrawlPolicy } from './crawl-policy.js';
import { CrawlRequest } from './crawl-request.js';
import { CrawlService } from './crawl.service.js';
import type { ReceivedMessage } from './interfaces/queue.interface.js';
import { ResultBatcher } from './result-batcher.js';

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
  private readonly limiter: ConcurrencyLimiter;
  private readonly batchers: Map<CrawlEnvironment, ResultBatcher>;
  private readonly inFlight = new Set<Promise<void>>();
  private consuming: Promise<unknown> = Promise.resolve();
  private isStopping = false;

  constructor(
    private readonly environments: CrawlEnvironments,
    configService: ConfigService<CrawlerConfig, true>,
    private readonly crawlService: CrawlService
  ) {
    this.limiter = new ConcurrencyLimiter(configService.get('concurrency', { infer: true }));
    this.batchers = new Map(
      environments.all.map((environment) => {
        return [environment, new ResultBatcher(environment.resultQueue, environment.requestQueue)];
      })
    );
  }

  onApplicationBootstrap(): void {
    this.consuming = Promise.all(this.environments.all.map((environment) => this.consume(environment)));
  }

  /** 새 메시지를 받지 않고, 처리 중인 크롤링과 모아둔 결과 발행이 끝날 때까지 기다림 */
  async beforeApplicationShutdown(): Promise<void> {
    this.isStopping = true;
    await this.consuming;
    await Promise.allSettled(this.inFlight);
    await this.closeBatchers();
  }

  /** 예외를 밖으로 던지지 않음. 완료 처리하지 않은 메시지는 큐가 다시 전달함 */
  private async handle(environment: CrawlEnvironment, message: ReceivedMessage): Promise<void> {
    const request = CrawlRequest.from(message.content);
    if (request === undefined) {
      this.logger.error(
        `[${environment.name}] 형식이 잘못된 크롤링 요청. dead letter queue 로 이동할 때까지 완료 처리하지 않음`,
        { content: message.content, deliveryCount: message.deliveryCount }
      );
      return;
    }

    try {
      const result = await this.crawlService.crawl(request, environment.bucketName);
      this.batchers.get(environment)?.add(JSON.stringify(result), message.receipt);
    } catch (error) {
      this.logger.error(`[${environment.name}] 크롤링 실패. 재전달 후 다시 처리함`, {
        towerId: request.towerId,
        scheduledAt: request.scheduledAt,
        deliveryCount: message.deliveryCount,
        error
      });
    }
  }

  private async consume(environment: CrawlEnvironment): Promise<void> {
    while (!this.isStopping) {
      await this.limiter.whenAvailable();

      let messages: ReceivedMessage[];
      try {
        messages = await environment.requestQueue.receive(this.receiveLimit());
      } catch (error) {
        this.logger.error(`[${environment.name}] 크롤링 요청 수신 실패`, error);
        await sleep(CrawlPolicy.QUEUE_RETRY_DELAY_MILLIS);
        continue;
      }

      // 중지 중에 받은 메시지는 처리하지 않음. visibility 시간이 지나면 다시 전달됨
      if (this.isStopping) {
        return;
      }

      for (const message of messages) {
        await this.dispatch(environment, message);
      }
    }
  }

  private receiveLimit(): number {
    return Math.min(Math.max(this.limiter.availableSlots, 1), CrawlPolicy.QUEUE_BATCH_SIZE);
  }

  /** 자리가 날 때까지 기다린 뒤 처리를 시작하고, 끝날 때까지 기다리지는 않음 */
  private async dispatch(environment: CrawlEnvironment, message: ReceivedMessage): Promise<void> {
    const release = await this.limiter.acquire();
    const task = this.handle(environment, message).finally(() => {
      release();
      this.inFlight.delete(task);
    });
    this.inFlight.add(task);
  }

  private async closeBatchers(): Promise<void> {
    await Promise.all([...this.batchers.values()].map((batcher) => batcher.close()));
  }
}
