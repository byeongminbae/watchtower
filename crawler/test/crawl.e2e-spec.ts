import { setTimeout as sleep } from 'node:timers/promises';
import { Logger } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { Test, type TestingModule } from '@nestjs/testing';
import { CrawlModule } from '../src/crawl/module/crawl.module.js';
import { EnvironmentResource, EnvironmentResources } from '../src/crawl/environment-resources.js';
import { HTML_STORAGE, type HtmlStorage } from '../src/crawl/interfaces/html-storage.interface.js';
import { PAGE_FETCHER, type PageFetcher, PageFetchSuccess } from '../src/crawl/interfaces/page-fetcher.interface.js';
import type { ReceivedMessage, RequestQueue, ResultQueue } from '../src/crawl/interfaces/queue.interface.js';
import { InternalNetworkIsolationCheck } from '../src/external/network/internal-network-isolation-check.js';

class InMemoryRequestQueue implements RequestQueue {
  readonly completedReceipts: string[] = [];
  receiveCount = 0;

  constructor(private readonly messages: ReceivedMessage[]) {}

  async receive(limit: number): Promise<ReceivedMessage[]> {
    this.receiveCount++;
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

describe('크롤러 (e2e)', () => {
  let moduleRef: TestingModule;
  let devRequestQueue: InMemoryRequestQueue;
  let prodRequestQueue: InMemoryRequestQueue;
  let devResultQueue: InMemoryResultQueue;
  let prodResultQueue: InMemoryResultQueue;
  let savedBuckets: string[];
  let compileModule: (isolationCheck: Pick<InternalNetworkIsolationCheck, 'onModuleInit'>) => Promise<TestingModule>;
  const passingIsolationCheck = { onModuleInit: async () => undefined };

  beforeEach(async () => {
    vi.spyOn(Logger.prototype, 'error').mockImplementation(() => {});
    devRequestQueue = new InMemoryRequestQueue([]);
    prodRequestQueue = new InMemoryRequestQueue([
      {
        content: JSON.stringify({
          towerId: 7,
          url: 'https://example.com',
          scheduledAt: '2026-10-30T00:00:00.000Z',
          triggeredByMemberId: null
        }),
        receipt: 'prod-1',
        deliveryCount: 1
      }
    ]);
    devResultQueue = new InMemoryResultQueue();
    prodResultQueue = new InMemoryResultQueue();
    savedBuckets = [];

    const pageFetcher: PageFetcher = {
      fetchPage: async () => {
        return new PageFetchSuccess({ httpStatusCode: 200, finalUrl: 'https://example.com/', html: '<html></html>' });
      }
    };
    const htmlStorage: HtmlStorage = {
      saveHtml: async (bucketName) => {
        savedBuckets.push(bucketName);
      }
    };

    compileModule = (isolationCheck) =>
      Test.createTestingModule({
        imports: [ConfigModule.forRoot({ isGlobal: true, load: [() => ({ concurrency: 2 })] }), CrawlModule]
      })
        // 호스트 방화벽 검사는 실제 인스턴스 메타데이터 주소에 연결을 시도하므로 테스트마다 검사 결과를 정해서 넣음
        .overrideProvider(InternalNetworkIsolationCheck)
        .useValue(isolationCheck)
        .overrideProvider(PAGE_FETCHER)
        .useValue(pageFetcher)
        .overrideProvider(HTML_STORAGE)
        .useValue(htmlStorage)
        .overrideProvider(EnvironmentResources)
        .useValue(
          new EnvironmentResources([
            new EnvironmentResource('dev', devRequestQueue, devResultQueue, 'watchtower-snapshot-dev'),
            new EnvironmentResource('prod', prodRequestQueue, prodResultQueue, 'watchtower-snapshot-prod')
          ])
        )
        .compile();
    moduleRef = await compileModule(passingIsolationCheck);
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
      expect.objectContaining({ towerId: 7, resultStatus: 'FETCHED', httpStatusCode: 200 })
    ]);
    expect(devResultQueue.published).toEqual([]);
    expect(prodRequestQueue.completedReceipts).toEqual(['prod-1']);
  });

  it('호스트 방화벽 검사가 실패하면 기동을 멈추고 큐에서 요청을 하나도 받지 않음', async () => {
    // given
    const failingIsolationCheck = {
      onModuleInit: async () => {
        throw new Error('크롤러 컨테이너가 인스턴스 메타데이터에 접근할 수 있어 기동을 멈춤');
      }
    };
    const blockedModuleRef = await compileModule(failingIsolationCheck);

    // when
    const starting = blockedModuleRef.init();

    // then
    await expect(starting).rejects.toThrow('인스턴스 메타데이터');
    // 수신 루프가 시작됐다면 첫 받기 요청을 보낼 만큼 기다린 뒤 확인함
    await sleep(100);
    expect(prodRequestQueue.receiveCount).toBe(0);
    expect(savedBuckets).toEqual([]);
  });
});
