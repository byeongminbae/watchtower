import { setTimeout as sleep } from 'node:timers/promises';
import { Test, type TestingModule } from '@nestjs/testing';
import { SimpleAuthenticationDetailsProvider } from 'oci-common';
import { AppModule } from '../src/app.module.js';
import { CrawlerConfig } from '../src/config/crawler-config.js';
import { CrawlEnvironment, CrawlEnvironments } from '../src/crawl/crawl-environment.js';
import { CrawlWorker } from '../src/crawl/crawl.worker.js';
import { HTML_STORAGE, type HtmlStorage } from '../src/crawl/interfaces/html-storage.interface.js';
import { PAGE_FETCHER, type PageFetcher } from '../src/crawl/interfaces/page-fetcher.interface.js';
import type { ReceivedMessage, RequestQueue, ResultQueue } from '../src/crawl/interfaces/queue.interface.js';

class InMemoryRequestQueue implements RequestQueue {
  readonly completedReceipts: string[] = [];

  constructor(private readonly messages: ReceivedMessage[]) {}

  async receive(limit: number): Promise<ReceivedMessage[]> {
    if (this.messages.length === 0) {
      await sleep(10);
      return [];
    }
    return this.messages.splice(0, limit);
  }

  async completeAll(receipts: string[]): Promise<void> {
    this.completedReceipts.push(...receipts);
  }
}

class InMemoryResultQueue implements ResultQueue {
  readonly published: string[] = [];

  async publishAll(contents: string[]): Promise<void> {
    this.published.push(...contents);
  }
}

function createConfig(): CrawlerConfig {
  const env: NodeJS.ProcessEnv = {
    OCI_REGION: 'ap-osaka-1',
    OCI_TENANCY_ID: 'e2e',
    OCI_USER_ID: 'e2e',
    OCI_FINGERPRINT: 'e2e',
    OCI_PRIVATE_KEY: 'e2e',
    OBJECT_STORAGE_NAMESPACE: 'e2e',
    CRAWLER_CONCURRENCY: '2'
  };
  for (const name of CrawlerConfig.ENVIRONMENT_NAMES) {
    const suffix = name.toUpperCase();
    env[`CRAWL_REQUEST_QUEUE_ID_${suffix}`] = `request-${name}`;
    env[`CRAWL_REQUEST_QUEUE_ENDPOINT_${suffix}`] = 'http://127.0.0.1:9';
    env[`CRAWL_RESULT_QUEUE_ID_${suffix}`] = `result-${name}`;
    env[`CRAWL_RESULT_QUEUE_ENDPOINT_${suffix}`] = 'http://127.0.0.1:9';
    env[`OBJECT_STORAGE_BUCKET_${suffix}`] = `watchtower-snapshot-${name}`;
  }
  return CrawlerConfig.from(env);
}

describe('크롤러 (e2e)', () => {
  let moduleRef: TestingModule;
  let devRequestQueue: InMemoryRequestQueue;
  let prodRequestQueue: InMemoryRequestQueue;
  let devResultQueue: InMemoryResultQueue;
  let prodResultQueue: InMemoryResultQueue;
  let savedBuckets: string[];

  beforeEach(async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
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

    moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(CrawlerConfig)
      .useValue(createConfig())
      .overrideProvider(SimpleAuthenticationDetailsProvider)
      .useValue({})
      .overrideProvider(PAGE_FETCHER)
      .useValue(pageFetcher)
      .overrideProvider(HTML_STORAGE)
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
    // given
    const worker = moduleRef.get(CrawlWorker);

    // when
    worker.start();
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
