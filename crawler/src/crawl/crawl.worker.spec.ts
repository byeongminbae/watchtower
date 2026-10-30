import { Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import type { Config } from '../config/config.js';
import { CrawlEnvironment, CrawlEnvironments } from './crawl-environment.js';
import { CrawlError, type CrawlResult, FailedCrawlResult } from './crawl-result.js';
import { CrawlRequest } from './crawl-request.js';
import type { CrawlService } from './crawl.service.js';
import { CrawlWorker } from './crawl.worker.js';
import type { ReceivedMessage, RequestQueue } from './interfaces/queue.interface.js';

const message: ReceivedMessage = {
  content: JSON.stringify({ towerId: 1, url: 'https://example.com', scheduledAt: '2026-10-30T00:00:00.000Z' }),
  receipt: 'receipt-1',
  deliveryCount: 1
};

const result: CrawlResult = FailedCrawlResult.of(
  CrawlRequest.from(message.content)!,
  new Date('2026-10-30T00:00:01.000Z'),
  new Date('2026-10-30T00:00:02.000Z'),
  CrawlError.NAVIGATION_TIMEOUT
);

const configService = new ConfigService<Config, true>({ concurrency: 2 });

/** 처음 한 번만 주어진 메시지를 돌려주고, 이후에는 메시지가 없는 큐처럼 동작함 */
function createRequestQueue(messages: ReceivedMessage[]): RequestQueue {
  return {
    receive: vi.fn(async () => {
      if (messages.length > 0) {
        return messages.splice(0);
      }
      await new Promise((resolve) => setTimeout(resolve, 5));
      return [];
    }),
    completeAll: vi.fn(async () => {})
  };
}

function createEnvironment(name: CrawlEnvironment['name'], messages: ReceivedMessage[] = []): CrawlEnvironment {
  return new CrawlEnvironment(
    name,
    createRequestQueue(messages),
    { publishAll: vi.fn(async () => {}) },
    `watchtower-snapshot-${name}`
  );
}

describe('CrawlWorker', () => {
  beforeEach(() => {
    vi.spyOn(Logger.prototype, 'error').mockImplementation(() => {});
  });

  it('결과는 요청을 받은 환경의 버킷과 결과 큐로만 보내고 요청을 완료 처리함', async () => {
    // given
    const dev = createEnvironment('dev');
    const prod = createEnvironment('prod', [message]);
    const crawlService = { crawl: vi.fn(async () => result) } as unknown as CrawlService;
    const worker = new CrawlWorker(new CrawlEnvironments([dev, prod]), configService, crawlService);

    // when
    worker.onApplicationBootstrap();
    await vi.waitFor(() => expect(crawlService.crawl).toHaveBeenCalled());
    await worker.beforeApplicationShutdown();

    // then
    expect(crawlService.crawl).toHaveBeenCalledWith(
      expect.objectContaining({ towerId: 1 }),
      'watchtower-snapshot-prod'
    );
    expect(prod.resultQueue.publishAll).toHaveBeenCalledWith([JSON.stringify(result)]);
    expect(prod.requestQueue.completeAll).toHaveBeenCalledWith(['receipt-1']);
    expect(dev.resultQueue.publishAll).not.toHaveBeenCalled();
  });

  it('일시적인 오류로 크롤링하지 못하면 결과를 발행하지 않고 요청을 남겨 다시 전달받음', async () => {
    // given
    const dev = createEnvironment('dev', [message]);
    const crawlService = {
      crawl: vi.fn(async () => {
        throw new Error('Object Storage 장애');
      })
    } as unknown as CrawlService;
    const worker = new CrawlWorker(new CrawlEnvironments([dev]), configService, crawlService);

    // when
    worker.onApplicationBootstrap();
    await vi.waitFor(() => expect(crawlService.crawl).toHaveBeenCalled());
    await worker.beforeApplicationShutdown();

    // then
    expect(dev.resultQueue.publishAll).not.toHaveBeenCalled();
    expect(dev.requestQueue.completeAll).not.toHaveBeenCalled();
  });

  it('형식이 잘못된 요청은 크롤링하지 않고 dead letter queue 로 가도록 완료 처리하지 않음', async () => {
    // given
    const dev = createEnvironment('dev', [{ ...message, content: 'not json' }]);
    const crawlService = { crawl: vi.fn(async () => result) } as unknown as CrawlService;
    const worker = new CrawlWorker(new CrawlEnvironments([dev]), configService, crawlService);

    // when
    worker.onApplicationBootstrap();
    await vi.waitFor(() => expect(vi.mocked(dev.requestQueue.receive).mock.calls.length).toBeGreaterThanOrEqual(2));
    await worker.beforeApplicationShutdown();

    // then
    expect(crawlService.crawl).not.toHaveBeenCalled();
    expect(dev.requestQueue.completeAll).not.toHaveBeenCalled();
  });

  it('모든 환경의 큐를 받되 동시 처리 한도는 환경끼리 함께 씀', async () => {
    // given
    const dev = createEnvironment('dev', [
      { ...message, receipt: 'dev-1' },
      { ...message, receipt: 'dev-2' }
    ]);
    const prod = createEnvironment('prod', [{ ...message, receipt: 'prod-1' }]);
    let running = 0;
    let maxRunning = 0;
    const crawlService = {
      crawl: vi.fn(async () => {
        running++;
        maxRunning = Math.max(maxRunning, running);
        await new Promise((resolve) => setTimeout(resolve, 10));
        running--;
        return result;
      })
    } as unknown as CrawlService;
    const worker = new CrawlWorker(new CrawlEnvironments([dev, prod]), configService, crawlService);

    // when
    worker.onApplicationBootstrap();
    await vi.waitFor(() => expect(crawlService.crawl).toHaveBeenCalledTimes(3));
    await worker.beforeApplicationShutdown();

    // then
    expect(maxRunning).toBe(configService.get('concurrency', { infer: true }));
    expect(dev.requestQueue.completeAll).toHaveBeenCalledWith(['dev-1', 'dev-2']);
    expect(prod.requestQueue.completeAll).toHaveBeenCalledWith(['prod-1']);
  });
});
