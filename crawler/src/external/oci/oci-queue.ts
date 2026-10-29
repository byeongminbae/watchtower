import type { QueueClient } from "oci-queue"
import { CrawlPolicy } from "../../crawl/crawl-policy.ts"
import type { ReceivedMessage, RequestQueue, ResultQueue } from "../../crawl/crawl-environment.ts"

/** 큐 하나에 대한 메시지 수신, 발행, 완료 처리. 큐마다 메시지 엔드포인트가 다를 수 있어 클라이언트를 따로 받음 */
export class OciQueue implements RequestQueue, ResultQueue {
  constructor(
    private readonly client: QueueClient,
    private readonly queueId: string
  ) {}

  async receive(limit: number): Promise<ReceivedMessage[]> {
    const response = await this.client.getMessages({
      queueId: this.queueId,
      limit,
      visibilityInSeconds: CrawlPolicy.MESSAGE_VISIBILITY_SECONDS,
      timeoutInSeconds: CrawlPolicy.QUEUE_POLLING_TIMEOUT_SECONDS
    })
    return response.getMessages.messages.map(({ content, receipt, deliveryCount }) => {
      return { content, receipt, deliveryCount }
    })
  }

  async publishAll(contents: string[]): Promise<void> {
    if (contents.length === 0) {
      return
    }
    await this.client.putMessages({
      queueId: this.queueId,
      putMessagesDetails: { messages: contents.map((content) => ({ content })) }
    })
  }

  async completeAll(receipts: string[]): Promise<void> {
    if (receipts.length === 0) {
      return
    }
    const response = await this.client.deleteMessages({
      queueId: this.queueId,
      deleteMessagesDetails: { entries: receipts.map((receipt) => ({ receipt })) }
    })

    const { serverFailures, clientFailures } = response.deleteMessagesResult
    if (serverFailures + clientFailures > 0) {
      console.error(`요청 ${serverFailures + clientFailures}건 완료 처리 실패. 같은 결과가 다시 발행될 수 있음`, {
        errors: response.deleteMessagesResult.entries.filter((entry) => entry.errorCode !== undefined)
      })
    }
  }
}
