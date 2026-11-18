import { TimeProvider } from '../../src/common/time-provider.js';
import { CrawlService } from '../../src/crawl/crawl.service.js';
import { CrawlRequestMessage } from '../../src/crawl/dto/crawl-request-message.dto.js';
import { FetchedPage, type PageFetcher, PageFetchFailure } from '../../src/crawl/interfaces/page-fetcher.js';
import type { SnapshotStorage } from '../../src/crawl/interfaces/snapshot-storage.js';

const extraction = { detectionAreaSelector: null, collectImageHashes: true, ignoreDateTime: false };

function createRequest(overrides: Record<string, unknown> = {}): CrawlRequestMessage {
  return CrawlRequestMessage.from(
    JSON.stringify({
      towerId: 7,
      url: 'https://example.com',
      extraction,
      triggeredByMemberId: 21,
      previousTextHash: null,
      previousImageHash: null,
      jobStartedAt: JOB_STARTED_AT,
      ...overrides
    })
  )!;
}

const TEXT_HASH = 'cc9d0c783b40ec911451311539bb98451925f5be4fe1492b50d8820c37d0af83';
const TEXT_WITH_DATE_TIME = '재입고 알림 2026-11-06 14:30';
const TEXT_WITH_DATE_TIME_HASH = 'ded1db96bfc14265fd339615fadacf515a86ab3910a882e9412e8fbd023fd5d8';
const DATE_TIME_IGNORED_TEXT_HASH = 'c1006af4dabf32a90b6cc4433718f6c8550c674185b73f35c932e7e84e95f4c4';
const IMAGE_HASH = 'a0fab1377f49a759b57f63318262ebe89fabfc990e8e93ceac2984561482b9d4';
const JOB_STARTED_AT = '2026-10-30T00:00:00.123456Z';
const crawlingStartedAt = new Date('2026-10-30T00:00:01.000Z');
const crawlingEndedAt = new Date('2026-10-30T00:00:03.000Z');
const TEXT_OBJECT_NAME = 'towers/7/2026-10-30T00:00:01.000Z.txt';

function createTimeProvider(): TimeProvider {
  const timeProvider = new TimeProvider();
  vi.spyOn(timeProvider, 'now').mockReturnValueOnce(crawlingStartedAt).mockReturnValue(crawlingEndedAt);
  return timeProvider;
}

function fetcherReturning(text: string): PageFetcher {
  return {
    fetchPage: vi.fn(async () => {
      return new FetchedPage('https://example.com/moved', 404, text, ['b'.repeat(64)]);
    })
  };
}

function createSnapshotStorage(): SnapshotStorage {
  return { saveText: vi.fn(async () => {}) };
}

describe('CrawlService', () => {
  it('직전 버전이 없는 페이지를 받아오면 크롤링 시작 시각으로 텍스트를 저장하고 받아온 내용을 담은 결과를 돌려줌', async () => {
    // given
    const request = createRequest();
    const pageFetcher = fetcherReturning('재입고 알림');
    const snapshotStorage = createSnapshotStorage();
    const crawlService = new CrawlService(pageFetcher, createTimeProvider());

    // when
    const result = await crawlService.crawl(request, snapshotStorage);

    // then
    expect(pageFetcher.fetchPage).toHaveBeenCalledWith('https://example.com', extraction);
    expect(snapshotStorage.saveText).toHaveBeenCalledWith(TEXT_OBJECT_NAME, '재입고 알림');
    expect(result).toEqual({
      towerId: 7,
      requestedUrl: 'https://example.com',
      finalUrl: 'https://example.com/moved',
      jobStartedAt: JOB_STARTED_AT,
      crawlingStartedAt,
      crawlingEndedAt,
      triggeredByMemberId: 21,
      crawlerResultCode: 'FETCHED',
      pageHttpStatusCode: 404,
      pageContent: {
        textHash: TEXT_HASH,
        dateTimeIgnoredTextHash: TEXT_HASH,
        detectionAreaSelector: null,
        textObjectName: TEXT_OBJECT_NAME,
        imageHash: IMAGE_HASH
      }
    });
  });

  it('요청받은 감시 영역으로 페이지를 받아오고 결과에 그 영역을 그대로 돌려줌', async () => {
    // given
    const request = createRequest({ extraction: { ...extraction, detectionAreaSelector: '.stock' } });
    const pageFetcher = fetcherReturning('재고 3개');
    const crawlService = new CrawlService(pageFetcher, createTimeProvider());

    // when
    const result = await crawlService.crawl(request, createSnapshotStorage());

    // then
    expect(pageFetcher.fetchPage).toHaveBeenCalledWith('https://example.com', {
      ...extraction,
      detectionAreaSelector: '.stock'
    });
    expect(result.pageContent?.detectionAreaSelector).toBe('.stock');
  });

  it('받아온 텍스트와 이미지가 모두 직전 버전과 같으면 저장하지 않고 저장 위치 없이 해시만 담은 결과를 돌려줌', async () => {
    // given
    const request = createRequest({ previousTextHash: TEXT_HASH, previousImageHash: IMAGE_HASH });
    const snapshotStorage = createSnapshotStorage();
    const crawlService = new CrawlService(fetcherReturning('재입고 알림'), createTimeProvider());

    // when
    const result = await crawlService.crawl(request, snapshotStorage);

    // then
    expect(snapshotStorage.saveText).not.toHaveBeenCalled();
    expect(result).toMatchObject({
      crawlerResultCode: 'FETCHED',
      pageContent: { textHash: TEXT_HASH, textObjectName: null, imageHash: IMAGE_HASH }
    });
  });

  it('텍스트는 직전 버전과 같아도 이미지가 바뀌면 직전 버전의 객체를 쓰지 않도록 텍스트를 새로 저장함', async () => {
    // given
    const request = createRequest({ previousTextHash: TEXT_HASH, previousImageHash: 'd'.repeat(64) });
    const snapshotStorage = createSnapshotStorage();
    const crawlService = new CrawlService(fetcherReturning('재입고 알림'), createTimeProvider());

    // when
    const result = await crawlService.crawl(request, snapshotStorage);

    // then
    expect(snapshotStorage.saveText).toHaveBeenCalledWith(TEXT_OBJECT_NAME, '재입고 알림');
    expect(result).toMatchObject({
      pageContent: { textHash: TEXT_HASH, textObjectName: TEXT_OBJECT_NAME, imageHash: IMAGE_HASH }
    });
  });

  it('날짜와 시간을 무시하면 날짜와 시간을 뺀 해시로 직전 버전과 비교해 날짜와 시간만 바뀐 텍스트는 저장하지 않음', async () => {
    // given
    const request = createRequest({
      extraction: { ...extraction, ignoreDateTime: true },
      previousTextHash: DATE_TIME_IGNORED_TEXT_HASH,
      previousImageHash: IMAGE_HASH
    });
    const snapshotStorage = createSnapshotStorage();
    const crawlService = new CrawlService(fetcherReturning(TEXT_WITH_DATE_TIME), createTimeProvider());

    // when
    const result = await crawlService.crawl(request, snapshotStorage);

    // then
    expect(snapshotStorage.saveText).not.toHaveBeenCalled();
    expect(result).toMatchObject({
      pageContent: {
        textHash: TEXT_WITH_DATE_TIME_HASH,
        dateTimeIgnoredTextHash: DATE_TIME_IGNORED_TEXT_HASH,
        textObjectName: null
      }
    });
  });

  it('날짜와 시간을 무시하지 않으면 원문 해시로 비교해 날짜와 시간만 바뀌어도 저장하고 두 해시를 모두 담음', async () => {
    // given
    const request = createRequest({
      previousTextHash: DATE_TIME_IGNORED_TEXT_HASH,
      previousImageHash: IMAGE_HASH
    });
    const snapshotStorage = createSnapshotStorage();
    const crawlService = new CrawlService(fetcherReturning(TEXT_WITH_DATE_TIME), createTimeProvider());

    // when
    const result = await crawlService.crawl(request, snapshotStorage);

    // then
    expect(snapshotStorage.saveText).toHaveBeenCalledWith(TEXT_OBJECT_NAME, TEXT_WITH_DATE_TIME);
    expect(result).toMatchObject({
      pageContent: {
        textHash: TEXT_WITH_DATE_TIME_HASH,
        dateTimeIgnoredTextHash: DATE_TIME_IGNORED_TEXT_HASH,
        textObjectName: TEXT_OBJECT_NAME
      }
    });
  });

  it('날짜와 시간을 무시해도 저장하는 텍스트는 원문 그대로임', async () => {
    // given
    const request = createRequest({ extraction: { ...extraction, ignoreDateTime: true } });
    const snapshotStorage = createSnapshotStorage();
    const crawlService = new CrawlService(fetcherReturning(TEXT_WITH_DATE_TIME), createTimeProvider());

    // when
    await crawlService.crawl(request, snapshotStorage);

    // then
    expect(snapshotStorage.saveText).toHaveBeenCalledWith(TEXT_OBJECT_NAME, TEXT_WITH_DATE_TIME);
  });

  it('페이지를 받아오지 못하면 아무것도 저장하지 않고 실패 이유를 결과 코드로 담은 결과를 돌려줌', async () => {
    // given
    const pageFetcher: PageFetcher = {
      fetchPage: async () => {
        return new PageFetchFailure('URL_NOT_ALLOWED');
      }
    };
    const snapshotStorage = createSnapshotStorage();
    const crawlService = new CrawlService(pageFetcher, createTimeProvider());

    // when
    const result = await crawlService.crawl(createRequest(), snapshotStorage);

    // then
    expect(snapshotStorage.saveText).not.toHaveBeenCalled();
    expect(result).toMatchObject({
      crawlerResultCode: 'URL_NOT_ALLOWED',
      triggeredByMemberId: 21,
      finalUrl: null,
      pageHttpStatusCode: null,
      pageContent: null
    });
  });
});
