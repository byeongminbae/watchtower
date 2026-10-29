import type { CrawlError } from './crawl-error.js';
import type { CrawlRequest } from './crawl-request.js';

/**
 * 크롤러가 결과 큐에 넣는 크롤링 결과. 필드가 그대로 JSON 으로 직렬화됨
 *
 * 4xx, 5xx 응답도 페이지를 받아온 것이므로 FETCHED 로 보냄. 접근 가능 여부와 변경 여부는 백엔드가 판단함
 */
export type CrawlResult = FetchedCrawlResult | FailedCrawlResult;

abstract class CrawlResultBase {
  readonly towerId: number;
  readonly url: string;
  readonly scheduledAt: string;
  readonly startedAt: string;
  readonly endedAt: string;

  protected constructor(request: CrawlRequest, startedAt: Date, endedAt: Date) {
    this.towerId = request.towerId;
    this.url = request.url;
    this.scheduledAt = request.scheduledAt;
    this.startedAt = startedAt.toISOString();
    this.endedAt = endedAt.toISOString();
  }
}

export class FetchedCrawlResult extends CrawlResultBase {
  readonly outcome = 'FETCHED';

  private constructor(
    request: CrawlRequest,
    startedAt: Date,
    endedAt: Date,
    readonly httpStatusCode: number,
    readonly finalUrl: string,
    readonly htmlObjectName: string
  ) {
    super(request, startedAt, endedAt);
  }

  static of(
    request: CrawlRequest,
    startedAt: Date,
    endedAt: Date,
    page: { httpStatusCode: number; finalUrl: string },
    htmlObjectName: string
  ): FetchedCrawlResult {
    return new FetchedCrawlResult(request, startedAt, endedAt, page.httpStatusCode, page.finalUrl, htmlObjectName);
  }
}

export class FailedCrawlResult extends CrawlResultBase {
  readonly outcome = 'FAILED';
  readonly errorCode: string;

  private constructor(request: CrawlRequest, startedAt: Date, endedAt: Date, error: CrawlError) {
    super(request, startedAt, endedAt);
    this.errorCode = error.code;
  }

  static of(request: CrawlRequest, startedAt: Date, endedAt: Date, error: CrawlError): FailedCrawlResult {
    return new FailedCrawlResult(request, startedAt, endedAt, error);
  }
}
