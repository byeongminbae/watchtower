import { Injectable } from "@nestjs/common"
import { TimeProvider } from "../common/time-provider.ts"
import type { CrawlError } from "./crawl-error.ts"
import type { CrawlRequest } from "./crawl-request.ts"
import { type CrawlResult, FailedCrawlResult, FetchedCrawlResult } from "./crawl-result.ts"

export type FetchedPage = {
  httpStatusCode: number
  finalUrl: string
  html: string
}

export type PageFetchOutcome =
  | { kind: "fetched", page: FetchedPage }
  | { kind: "failed", error: CrawlError }

export abstract class PageFetcher {
  /** 재시도해도 결과가 같은 실패는 failed 로 돌려주고, 브라우저 종료 같은 일시적인 오류만 예외로 던짐 */
  abstract fetchPage(url: string): Promise<PageFetchOutcome>
}

export abstract class HtmlStorage {
  abstract saveHtml(bucketName: string, objectName: string, html: string): Promise<void>
}

@Injectable()
export class CrawlService {
  constructor(
    private readonly pageFetcher: PageFetcher,
    private readonly htmlStorage: HtmlStorage,
    private readonly timeProvider: TimeProvider
  ) {}

  async crawl(request: CrawlRequest, bucketName: string): Promise<CrawlResult> {
    const startedAt = this.timeProvider.now()
    const outcome = await this.pageFetcher.fetchPage(request.url)

    if (outcome.kind === "failed") {
      return FailedCrawlResult.of(request, startedAt, this.timeProvider.now(), outcome.error)
    }

    const htmlObjectName = request.toHtmlObjectName()
    await this.htmlStorage.saveHtml(bucketName, htmlObjectName, outcome.page.html)
    return FetchedCrawlResult.of(request, startedAt, this.timeProvider.now(), outcome.page, htmlObjectName)
  }
}
