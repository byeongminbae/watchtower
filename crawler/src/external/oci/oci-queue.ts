import { Logger } from '@nestjs/common';
import type { AuthenticationDetailsProvider } from 'oci-common';
import { QueueClient, type models } from 'oci-queue';
import { Policy } from '../../config/policy.js';
import type { ReceivedMessage, RequestQueue, ResultQueue } from '../../crawl/interfaces/queue.interface.js';

/** OCI Queue 하나에서 메시지를 받고, 넣고, 완료 처리(삭제)함. 큐마다 메시지 엔드포인트가 다를 수 있어 큐마다 클라이언트를 따로 둠 */
export class OciQueue implements RequestQueue, ResultQueue {
  private static readonly logger = new Logger(OciQueue.name);

  private constructor(
    private readonly queueClient: QueueClient,
    private readonly queueId: string
  ) {}

  static of(authenticationDetailsProvider: AuthenticationDetailsProvider, queueId: string, endpoint: string): OciQueue {
    const queueClient = new QueueClient({ authenticationDetailsProvider });
    queueClient.endpoint = endpoint;
    return new OciQueue(queueClient, queueId);
  }

  async receive(maxMessageCount: number): Promise<ReceivedMessage[]> {
    const response = await this.queueClient.getMessages({
      queueId: this.queueId,
      limit: maxMessageCount,
      visibilityInSeconds: Policy.MESSAGE_VISIBILITY_SECONDS,
      timeoutInSeconds: Policy.QUEUE_POLLING_TIMEOUT_SECONDS
    });
    return response.getMessages.messages.map(({ content, receipt, deliveryCount }) => {
      return { content, receipt, deliveryCount };
    });
  }

  async publishAll(messageContents: string[]): Promise<void> {
    await this.queueClient.putMessages({
      queueId: this.queueId,
      putMessagesDetails: { messages: messageContents.map((content) => ({ content })) }
    });
  }

  async completeAll(messageReceipts: string[]): Promise<void> {
    const response = await this.queueClient.deleteMessages({
      queueId: this.queueId,
      deleteMessagesDetails: { entries: messageReceipts.map((receipt) => ({ receipt })) }
    });
    OciQueue.logFailedCompletions(response.deleteMessagesResult);
  }

  private static logFailedCompletions(deleteResult: models.DeleteMessagesResult): void {
    const failedCount = deleteResult.serverFailures + deleteResult.clientFailures;
    if (failedCount === 0) {
      return;
    }
    OciQueue.logger.error(`요청 ${failedCount}건 완료 처리 실패. 같은 결과가 다시 발행될 수 있음`, {
      errors: deleteResult.entries.filter((entry) => entry.errorCode !== undefined)
    });
  }
}
