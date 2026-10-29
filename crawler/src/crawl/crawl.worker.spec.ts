import type { CrawlerConfig } from '../config/crawler-config.js';
import { CrawlEnvironment, CrawlEnvironments } from './crawl-environment.js';
import type { ReceivedMessage } from './interfaces/request-queue.interface.js';
import { CrawlError } from './crawl-error.js';
import { CrawlRequest } from './crawl-request.js';
import { type CrawlResult, FailedCrawlResult } from './crawl-result.js';
import type { CrawlService } from './crawl.service.js';
import { CrawlWorker } from './crawl.worker.js';

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

const config = { concurrency: 2 } as CrawlerConfig;

function createEnvironment(name: CrawlEnvironment['name']): CrawlEnvironment {
  return new CrawlEnvironment(
    name,
    { receive: vi.fn(async () => []), completeAll: vi.fn(async () => {}) },
    { publishAll: vi.fn(async () => {}) },
    `watchtower-snapshot-${name}`
  );
}

describe('CrawlWorker.handle', () => {
  let dev: CrawlEnvironment;
  let prod: CrawlEnvironment;

  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    dev = createEnvironment('dev');
    prod = createEnvironment('prod');
  });

  it('결과는 요청을 받은 환경의 버킷과 결과 큐로만 보냄', async () => {
    // given
    const crawlService = { crawl: vi.fn(async () => result) } as unknown as CrawlService;
    const worker = new CrawlWorker(new CrawlEnvironments([dev, prod]), config, crawlService);

    // when
    await worker.handle(prod, message);
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
    const crawlService = {
      crawl: vi.fn(async () => {
        throw new Error('Object Storage 장애');
      })
    } as unknown as CrawlService;
    const worker = new CrawlWorker(new CrawlEnvironments([dev]), config, crawlService);

    // when
    await worker.handle(dev, message);
    await worker.beforeApplicationShutdown();

    // then
    expect(dev.resultQueue.publishAll).not.toHaveBeenCalled();
    expect(dev.requestQueue.completeAll).not.toHaveBeenCalled();
  });

  it('형식이 잘못된 요청은 크롤링하지 않고 dead letter queue 로 가도록 완료 처리하지 않음', async () => {
    // given
    const crawlService = { crawl: vi.fn(async () => result) } as unknown as CrawlService;
    const worker = new CrawlWorker(new CrawlEnvironments([dev]), config, crawlService);

    // when
    await worker.handle(dev, { ...message, content: 'not json' });
    await worker.beforeApplicationShutdown();

    // then
    expect(crawlService.crawl).not.toHaveBeenCalled();
    expect(dev.requestQueue.completeAll).not.toHaveBeenCalled();
  });
});

describe('CrawlWorker 수신 루프', () => {
  it('모든 환경의 큐를 받되 동시 처리 한도는 환경끼리 함께 씀', async () => {
    // given
    vi.spyOn(console, 'error').mockImplementation(() => {});
    const dev = createEnvironment('dev');
    const prod = createEnvironment('prod');
    let isFirstDevReceive = true;
    let isFirstProdReceive = true;
    dev.requestQueue.receive = vi.fn(async () => {
      if (!isFirstDevReceive) {
        return new Promise<ReceivedMessage[]>((resolve) => setTimeout(() => resolve([]), 5));
      }
      isFirstDevReceive = false;
      return [
        { ...message, receipt: 'dev-1' },
        { ...message, receipt: 'dev-2' }
      ];
    });
    prod.requestQueue.receive = vi.fn(async () => {
      if (!isFirstProdReceive) {
        return new Promise<ReceivedMessage[]>((resolve) => setTimeout(() => resolve([]), 5));
      }
      isFirstProdReceive = false;
      return [{ ...message, receipt: 'prod-1' }];
    });

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
    const worker = new CrawlWorker(new CrawlEnvironments([dev, prod]), config, crawlService);

    // when
    worker.start();
    await vi.waitFor(() => expect(crawlService.crawl).toHaveBeenCalledTimes(3));
    await worker.beforeApplicationShutdown();

    // then
    expect(maxRunning).toBe(config.concurrency);
    expect(dev.requestQueue.completeAll).toHaveBeenCalledWith(['dev-1', 'dev-2']);
    expect(prod.requestQueue.completeAll).toHaveBeenCalledWith(['prod-1']);
  });
});
