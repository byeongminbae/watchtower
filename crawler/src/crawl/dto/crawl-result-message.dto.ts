import type { CrawlRequestMessage } from './crawl-request-message.dto.js';

/**
 * 크롤링이 정상적으로 끝나지 못한 이유. 결과 메시지로 백엔드에 전달됨
 *
 * 재시도해도 결과가 같은 실패만 여기에 정의함. 일시적인 인프라 오류는 결과로 보내지 않고 메시지를 재전달 받아 다시 처리함
 */
export class CrawlError {
  static readonly URL_NOT_ALLOWED = new CrawlError('CRW_001', '내부망 등 허용되지 않는 주소로 접근하려 함');
  static readonly NAVIGATION_TIMEOUT = new CrawlError('CRW_002', '제한 시간 안에 페이지를 불러오지 못함');
  static readonly HTML_TOO_LARGE = new CrawlError('CRW_003', 'HTML 크기가 수집 한도를 넘음');
  static readonly NAVIGATION_FAILED = new CrawlError('CRW_004', 'DNS 조회 실패, 연결 거부 등으로 페이지를 열지 못함');

  private constructor(
    readonly code: string,
    private readonly description: string
  ) {}
}

/**
 * 크롤러가 결과 큐에 넣는 크롤링 결과. 필드가 그대로 JSON 으로 직렬화됨
 *
 * 4xx, 5xx 응답도 페이지를 받아온 것이므로 FETCHED 로 보냄. 접근 가능 여부와 변경 여부는 백엔드가 판단함
 */
export type CrawlResult = CrawlSuccessResultMessage | CrawlFailedResultMessage;

abstract class CrawlResultMessage {
  readonly towerId: number;
  readonly url: string;
  readonly scheduledAt: string;
  readonly crawlStartedAt: string;
  readonly crawlEndedAt: string;

  protected constructor(request: CrawlRequestMessage, startedAt: Date, endedAt: Date) {
    this.towerId = request.towerId;
    this.url = request.url;
    this.scheduledAt = request.scheduledAt;
    this.crawlStartedAt = startedAt.toISOString();
    this.crawlEndedAt = endedAt.toISOString();
  }
}

export class CrawlSuccessResultMessage extends CrawlResultMessage {
  readonly outcome = 'FETCHED';
  readonly httpStatusCode: number;
  readonly finalUrl: string;

  constructor(
    request: CrawlRequestMessage,
    startedAt: Date,
    endedAt: Date,
    fetchedPage: { httpStatusCode: number; finalUrl: string },
    readonly htmlObjectName: string
  ) {
    super(request, startedAt, endedAt);
    this.httpStatusCode = fetchedPage.httpStatusCode;
    this.finalUrl = fetchedPage.finalUrl;
  }
}

export class CrawlFailedResultMessage extends CrawlResultMessage {
  readonly outcome = 'FAILED';
  readonly errorCode: string;

  constructor(request: CrawlRequestMessage, startedAt: Date, endedAt: Date, error: CrawlError) {
    super(request, startedAt, endedAt);
    this.errorCode = error.code;
  }
}
