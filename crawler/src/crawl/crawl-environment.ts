import type { CrawlEnvironmentName } from "../config/crawler-config.ts"

export type ReceivedMessage = {
  content: string
  receipt: string
  deliveryCount: number
}

export abstract class RequestQueue {
  /** 메시지가 없으면 최대 폴링 대기 시간만큼 기다린 뒤 빈 배열을 돌려줌 */
  abstract receive(limit: number): Promise<ReceivedMessage[]>

  /** 지우지 못한 메시지는 visibility 시간이 지나면 다시 전달됨 */
  abstract completeAll(receipts: string[]): Promise<void>
}

export abstract class ResultQueue {
  /** 전부 넣거나 전부 실패함 */
  abstract publishAll(contents: string[]): Promise<void>
}

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
