import type { CrawlError } from '../dto/crawl-result-message.dto.js';

export type FetchedPage = {
  httpStatusCode: number;
  finalUrl: string;
  html: string;
};

export type PageFetchOutcome = { kind: 'fetched'; page: FetchedPage } | { kind: 'failed'; error: CrawlError };

export abstract class PageFetcher {
  /** 재시도해도 결과가 같은 실패는 failed 로 돌려주고, 브라우저 종료 같은 일시적인 오류만 예외로 던짐 */
  abstract fetchPage(url: string): Promise<PageFetchOutcome>;
}
