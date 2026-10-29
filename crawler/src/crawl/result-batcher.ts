import { CrawlPolicy } from "./crawl-policy.ts"
import type { RequestQueue, ResultQueue } from "./crawl-environment.ts"

/**
 * 결과를 모아 한 번에 발행하고, 발행에 성공한 결과의 요청만 한 번에 완료 처리함
 *
 * 발행에 실패하면 요청을 완료 처리하지 않으므로 큐가 다시 전달함. 결과가 유실되지 않는 대신 중복될 수 있음
 */
export class ResultBatcher {
  private readonly contents: string[] = []
  private readonly receipts: string[] = []
  private readonly flushes = new Set<Promise<void>>()
  private flushTimer: NodeJS.Timeout | undefined

  constructor(
    private readonly resultQueue: ResultQueue,
    private readonly requestQueue: RequestQueue
  ) {}

  add(content: string, receipt: string): void {
    this.contents.push(content)
    this.receipts.push(receipt)

    if (this.contents.length >= CrawlPolicy.QUEUE_BATCH_SIZE) {
      this.flush()
      return
    }
    this.flushTimer ??= setTimeout(() => this.flush(), CrawlPolicy.RESULT_FLUSH_DELAY_MILLIS)
  }

  /** 남은 결과를 모두 발행할 때까지 기다림 */
  async close(): Promise<void> {
    this.flush()
    await Promise.allSettled(this.flushes)
  }

  private flush(): void {
    clearTimeout(this.flushTimer)
    this.flushTimer = undefined
    if (this.contents.length === 0) {
      return
    }

    const contents = this.contents.splice(0, CrawlPolicy.QUEUE_BATCH_SIZE)
    const receipts = this.receipts.splice(0, CrawlPolicy.QUEUE_BATCH_SIZE)
    const flushing = this.publishThenComplete(contents, receipts).finally(() => this.flushes.delete(flushing))
    this.flushes.add(flushing)

    if (this.contents.length > 0) {
      this.flush()
    }
  }

  private async publishThenComplete(contents: string[], receipts: string[]): Promise<void> {
    try {
      await this.resultQueue.publishAll(contents)
    } catch (error) {
      console.error(`결과 ${contents.length}건 발행 실패. 요청을 다시 전달받아 처리함`, error)
      return
    }

    try {
      await this.requestQueue.completeAll(receipts)
    } catch (error) {
      console.error(`요청 ${receipts.length}건 완료 처리 실패. 같은 결과가 다시 발행될 수 있음`, error)
    }
  }
}
