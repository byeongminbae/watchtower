import type { CrawlEnvironmentName } from "../config/crawler-config.ts"
import type { RequestQueue } from "./interfaces/request-queue.ts"
import type { ResultQueue } from "./interfaces/result-queue.ts"

/**
 * 크롤러는 dev, prod 요청을 한 프로세스에서 처리하지만 결과가 섞이면 안 됨
 *
 * 환경은 메시지 내용이 아니라 요청을 받은 큐로 정함. 메시지에 환경을 적게 하면 dev 의 실수가 prod 타워에 반영될 수 있음
 */
export class CrawlEnvironment {
  constructor(
    readonly name: CrawlEnvironmentName,
    readonly requestQueue: RequestQueue,
    readonly resultQueue: ResultQueue,
    readonly bucketName: string
  ) {}
}

export class CrawlEnvironments {
  constructor(readonly all: CrawlEnvironment[]) {}
}
