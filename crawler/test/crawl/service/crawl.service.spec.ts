import { TimeProvider } from '../../../src/common/time-provider.js';
import { CrawlError } from '../../../src/crawl/dto/crawl-result-message.dto.js';
import { CrawlRequestMessage } from '../../../src/crawl/dto/crawl-request-message.dto.js';
import { CrawlService } from '../../../src/crawl/service/crawl.service.js';
import type { HtmlStorage } from '../../../src/crawl/interfaces/html-storage.interface.js';
import {
  type PageFetcher,
  PageFetchFailure,
  PageFetchSuccess
} from '../../../src/crawl/interfaces/page-fetcher.interface.js';

const request = CrawlRequestMessage.from(
  JSON.stringify({ towerId: 7, url: 'https://example.com', scheduledAt: '2026-10-30T00:00:00.000Z' })
)!;
const startedAt = new Date('2026-10-30T00:00:01.000Z');
const endedAt = new Date('2026-10-30T00:00:03.000Z');

function createTimeProvider(): TimeProvider {
  const timeProvider = new TimeProvider();
  vi.spyOn(timeProvider, 'now').mockReturnValueOnce(startedAt).mockReturnValue(endedAt);
  return timeProvider;
}

describe('CrawlService', () => {
  it('페이지를 받아오면 요청을 받은 환경의 버킷에 HTML 을 저장하고 저장 위치를 담은 결과를 돌려줌', async () => {
    // given
    const pageFetcher: PageFetcher = {
      fetchPage: async () => {
        return new PageFetchSuccess({ httpStatusCode: 404, finalUrl: 'https://example.com/', html: '<html></html>' });
      }
    };
    const htmlStorage: HtmlStorage = { saveHtml: vi.fn(async () => {}) };
    const crawlService = new CrawlService(pageFetcher, htmlStorage, createTimeProvider());

    // when
    const result = await crawlService.crawl(request, 'watchtower-snapshot-dev');

    // then
    expect(htmlStorage.saveHtml).toHaveBeenCalledWith(
      'watchtower-snapshot-dev',
      'towers/7/2026-10-30T00:00:00.000Z.html',
      '<html></html>'
    );
    expect(result).toMatchObject({
      towerId: 7,
      url: 'https://example.com',
      scheduledAt: '2026-10-30T00:00:00.000Z',
      crawlStartedAt: startedAt.toISOString(),
      crawlEndedAt: endedAt.toISOString(),
      outcome: 'FETCHED',
      httpStatusCode: 404,
      finalUrl: 'https://example.com/',
      htmlObjectName: 'towers/7/2026-10-30T00:00:00.000Z.html'
    });
  });

  it('페이지를 받아오지 못하면 아무것도 저장하지 않고 실패 이유를 담은 결과를 돌려줌', async () => {
    // given
    const pageFetcher: PageFetcher = {
      fetchPage: async () => {
        return new PageFetchFailure(CrawlError.URL_NOT_ALLOWED);
      }
    };
    const htmlStorage: HtmlStorage = { saveHtml: vi.fn(async () => {}) };
    const crawlService = new CrawlService(pageFetcher, htmlStorage, createTimeProvider());

    // when
    const result = await crawlService.crawl(request, 'watchtower-snapshot-dev');

    // then
    expect(htmlStorage.saveHtml).not.toHaveBeenCalled();
    expect(result).toMatchObject({ outcome: 'FAILED', errorCode: CrawlError.URL_NOT_ALLOWED.code });
  });
});
