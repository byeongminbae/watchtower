import type { Extraction } from '../dto/crawl-request-message.dto.js';
import type { CrawlFailureCode } from '../type/crawler-result-code.js';

/**
 * @property text 감시 영역에서 화면에 보이는 텍스트
 * @property imageHashes 감시 영역 안 이미지의 해시. 이미지를 수집하지 않았으면 null
 */
export class FetchedPage {
  readonly isFailed = false;

  constructor(
    readonly finalUrl: string,
    readonly httpStatusCode: number,
    readonly text: string,
    readonly imageHashes: readonly string[] | null,
    readonly screenshot: Buffer | null
  ) {}
}

export class PageFetchFailure {
  readonly isFailed = true;

  constructor(readonly code: CrawlFailureCode) {}
}

export type PageFetchResult = FetchedPage | PageFetchFailure;

export abstract class PageFetcher {
  /** 재시도해도 결과가 같은 실패는 [PageFetchFailure] 로 돌려주고, 브라우저 종료 같은 일시적인 오류만 예외로 던짐 */
  abstract fetchPage(url: string, extraction: Extraction): Promise<PageFetchResult>;
}
