import { Inject, Injectable } from "@nestjs/common"
import type { CrawlErrorType } from "./crawl-error.ts"
import type { CrawlRequest } from "./crawl-request.ts"
import { type CrawlResult, toFailedResult, toFetchedResult } from "./crawl-result.ts"

export const PAGE_FETCHER = Symbol("PAGE_FETCHER")
export const HTML_STORAGE = Symbol("HTML_STORAGE")
export const NOW = Symbol("NOW")

export type FetchedPage = {
  httpStatusCode: number
  finalUrl: string
  html: string
}

export type PageFetchOutcome =
  | { kind: "fetched", page: FetchedPage }
  | { kind: "failed", error: CrawlErrorType }

export type PageFetcher = {
  /** 재시도해도 결과가 같은 실패는 failed 로 돌려주고, 브라우저 종료 같은 일시적인 오류만 예외로 던짐 */
  fetchPage(url: string): Promise<PageFetchOutcome>
}

export type HtmlStorage = {
  saveHtml(bucketName: string, objectName: string, html: string): Promise<void>
}

@Injectable()
export class CrawlService {
  constructor(
    @Inject(PAGE_FETCHER) private readonly pageFetcher: PageFetcher,
    @Inject(HTML_STORAGE) private readonly htmlStorage: HtmlStorage,
    @Inject(NOW) private readonly now: () => Date
  ) {}

  async crawl(request: CrawlRequest, bucketName: string): Promise<CrawlResult> {
    const startedAt = this.now()
    const outcome = await this.pageFetcher.fetchPage(request.url)

    if (outcome.kind === "failed") {
      return toFailedResult(request, startedAt, this.now(), outcome.error)
    }

    const htmlObjectName = toHtmlObjectName(request)
    await this.htmlStorage.saveHtml(bucketName, htmlObjectName, outcome.page.html)
    return toFetchedResult(request, startedAt, this.now(), outcome.page, htmlObjectName)
  }
}

/** 같은 요청이 다시 처리되면 같은 객체를 덮어써서 저장소에 중복이 쌓이지 않게 함 */
export function toHtmlObjectName(request: CrawlRequest): string {
  return `towers/${request.towerId}/${request.scheduledAt}.html`
}
