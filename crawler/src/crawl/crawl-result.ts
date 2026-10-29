import type { CrawlErrorType } from "./crawl-error.ts"
import type { CrawlRequest } from "./crawl-request.ts"

/**
 * 크롤러가 결과 큐에 넣는 크롤링 결과
 *
 * 4xx, 5xx 응답도 페이지를 받아온 것이므로 FETCHED 로 보냄. 접근 가능 여부와 변경 여부는 백엔드가 판단함
 */
export type CrawlResult = FetchedCrawlResult | FailedCrawlResult

type CrawlResultBase = {
  towerId: number
  url: string
  scheduledAt: string
  startedAt: string
  endedAt: string
}

export type FetchedCrawlResult = CrawlResultBase & {
  outcome: "FETCHED"
  httpStatusCode: number
  finalUrl: string
  htmlObjectName: string
}

export type FailedCrawlResult = CrawlResultBase & {
  outcome: "FAILED"
  errorCode: string
}

export function toFetchedResult(
  request: CrawlRequest,
  startedAt: Date,
  endedAt: Date,
  page: { httpStatusCode: number, finalUrl: string },
  htmlObjectName: string
): FetchedCrawlResult {
  return {
    ...toResultBase(request, startedAt, endedAt),
    outcome: "FETCHED",
    httpStatusCode: page.httpStatusCode,
    finalUrl: page.finalUrl,
    htmlObjectName
  }
}

export function toFailedResult(
  request: CrawlRequest,
  startedAt: Date,
  endedAt: Date,
  error: CrawlErrorType
): FailedCrawlResult {
  return {
    ...toResultBase(request, startedAt, endedAt),
    outcome: "FAILED",
    errorCode: error.code
  }
}

function toResultBase(request: CrawlRequest, startedAt: Date, endedAt: Date): CrawlResultBase {
  return {
    towerId: request.towerId,
    url: request.url,
    scheduledAt: request.scheduledAt,
    startedAt: startedAt.toISOString(),
    endedAt: endedAt.toISOString()
  }
}
