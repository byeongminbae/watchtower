import { setTimeout as sleep } from 'node:timers/promises';
import { Logger } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { Test, type TestingModule } from '@nestjs/testing';
import { CrawlModule } from '../src/crawl/module/crawl.module.js';
import { CrawlEnvironment, CrawlEnvironments } from '../src/crawl/crawl-environment.model.js';
import { HtmlStorage } from '../src/crawl/interfaces/html-storage.interface.js';
import { PageFetcher } from '../src/crawl/interfaces/page-fetcher.interface.js';
import { type ReceivedMessage, RequestQueue, ResultQueue } from '../src/crawl/interfaces/queue.interface.js';

class InMemoryRequestQueue extends RequestQueue {
  readonly completedReceipts: string[] = [];

  constructor(private readonly messages: ReceivedMessage[]) {
    super();
  }

  override async receive(limit: number): Promise<ReceivedMessage[]> {
    if (this.messages.length === 0) {
      await sleep(10);
      return [];
    }
    return this.messages.splice(0, limit);
  }

  override async completeAll(receipts: string[]): Promise<void> {
    this.completedReceipts.push(...receipts);
  }
}

class InMemoryResultQueue extends ResultQueue {
  readonly published: string[] = [];

  override async publishAll(contents: string[]): Promise<void> {
    this.published.push(...contents);
  }
}

describe('크롤러 (e2e)', () => {
  let moduleRef: TestingModule;
  let devRequestQueue: InMemoryRequestQueue;
  let prodRequestQueue: InMemoryRequestQueue;
  let devResultQueue: InMemoryResultQueue;
  let prodResultQueue: InMemoryResultQueue;
  let savedBuckets: string[];

  beforeEach(async () => {
    vi.spyOn(Logger.prototype, 'error').mockImplementation(() => {});
    devRequestQueue = new InMemoryRequestQueue([]);
    prodRequestQueue = new InMemoryRequestQueue([
      {
        content: JSON.stringify({ towerId: 7, url: 'https://example.com', scheduledAt: '2026-10-30T00:00:00.000Z' }),
        receipt: 'prod-1',
        deliveryCount: 1
      }
    ]);
    devResultQueue = new InMemoryResultQueue();
    prodResultQueue = new InMemoryResultQueue();
    savedBuckets = [];

    const pageFetcher: PageFetcher = {
      fetchPage: async () => {
        return {
          kind: 'fetched',
          page: { httpStatusCode: 200, finalUrl: 'https://example.com/', html: '<html></html>' }
        };
      }
    };
    const htmlStorage: HtmlStorage = {
      saveHtml: async (bucketName) => {
        savedBuckets.push(bucketName);
      }
    };

    moduleRef = await Test.createTestingModule({
      imports: [ConfigModule.forRoot({ isGlobal: true, load: [() => ({ concurrency: 2 })] }), CrawlModule]
    })
      .overrideProvider(PageFetcher)
      .useValue(pageFetcher)
      .overrideProvider(HtmlStorage)
      .useValue(htmlStorage)
      .overrideProvider(CrawlEnvironments)
      .useValue(
        new CrawlEnvironments([
          new CrawlEnvironment('dev', devRequestQueue, devResultQueue, 'watchtower-snapshot-dev'),
          new CrawlEnvironment('prod', prodRequestQueue, prodResultQueue, 'watchtower-snapshot-prod')
        ])
      )
      .compile();
  });

  it('prod 요청 큐로 들어온 요청은 prod 버킷에 저장하고 prod 결과 큐로만 발행한 뒤 요청을 완료 처리함', async () => {
    // given: 요청 큐, 결과 큐, 브라우저, 저장소는 beforeEach 에서 가짜로 준비함

    // when
    await moduleRef.init();
    await vi.waitFor(() => expect(savedBuckets).toHaveLength(1));
    await moduleRef.close();

    // then
    expect(savedBuckets).toEqual(['watchtower-snapshot-prod']);
    expect(prodResultQueue.published.map((content) => JSON.parse(content))).toEqual([
      expect.objectContaining({ towerId: 7, outcome: 'FETCHED', httpStatusCode: 200 })
    ]);
    expect(devResultQueue.published).toEqual([]);
    expect(prodRequestQueue.completedReceipts).toEqual(['prod-1']);
  });
});
