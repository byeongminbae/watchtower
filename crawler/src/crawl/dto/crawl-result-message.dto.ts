import type { CrawlerResultCode, CrawlFailureCode } from '../type/crawler-result-code.js';
import type { FetchedPage } from '../interfaces/page-fetcher.js';
import type { ContentHashes } from '../vo/content-hashes.js';
import type { CrawlRequestMessage } from './crawl-request-message.dto.js';

type PageContent = {
  readonly textHash: string;
  readonly dateTimeIgnoredTextHash: string;
  readonly detectionAreaSelector: string | null;
  readonly textObjectName: string | null;
  readonly screenshotObjectName: string | null;
  readonly imageHash: string | null;
};

export type PageContentObjectNames = {
  readonly textObjectName: string;
  readonly screenshotObjectName: string | null;
};

/**
 * 크롤러가 결과 큐에 넣는 크롤링 결과. 필드가 그대로 JSON 으로 직렬화됨
 *
 * @property pageHttpStatusCode 감시 대상 페이지가 돌려준 HTTP 상태 코드. 응답을 받지 못했으면 null
 */
export class CrawlResultMessage {
  readonly towerId: number;
  readonly requestedUrl: string;
  readonly jobStartedAt: string;
  readonly triggeredByMemberId: number | null;

  private constructor(
    request: CrawlRequestMessage,
    readonly crawlingStartedAt: Date,
    readonly crawlingEndedAt: Date,
    readonly crawlerResultCode: CrawlerResultCode,
    readonly finalUrl: string | null,
    readonly pageHttpStatusCode: number | null,
    readonly pageContent: PageContent | null
  ) {
    this.towerId = request.towerId;
    this.requestedUrl = request.url;
    this.jobStartedAt = request.jobStartedAt;
    this.triggeredByMemberId = request.triggeredByMemberId;
  }

  static fetched(
    request: CrawlRequestMessage,
    crawlingStartedAt: Date,
    crawlingEndedAt: Date,
    page: FetchedPage,
    contentHashes: ContentHashes,
    pageContentObjectNames: PageContentObjectNames | null
  ): CrawlResultMessage {
    return new CrawlResultMessage(
      request,
      crawlingStartedAt,
      crawlingEndedAt,
      'FETCHED',
      page.finalUrl,
      page.httpStatusCode,
      {
        textHash: contentHashes.textHash,
        dateTimeIgnoredTextHash: contentHashes.dateTimeIgnoredTextHash,
        detectionAreaSelector: request.extraction.detectionAreaSelector,
        textObjectName: pageContentObjectNames?.textObjectName ?? null,
        screenshotObjectName: pageContentObjectNames?.screenshotObjectName ?? null,
        imageHash: contentHashes.imageHash
      }
    );
  }

  static failed(
    request: CrawlRequestMessage,
    crawlingStartedAt: Date,
    crawlingEndedAt: Date,
    failureCode: CrawlFailureCode
  ): CrawlResultMessage {
    return new CrawlResultMessage(request, crawlingStartedAt, crawlingEndedAt, failureCode, null, null, null);
  }
}
