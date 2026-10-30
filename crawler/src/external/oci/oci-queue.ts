import { Logger } from '@nestjs/common';
import type { AuthenticationDetailsProvider } from 'oci-common';
import { QueueClient, type models } from 'oci-queue';
import { Policy } from '../../config/policy.js';
import { type ReceivedMessage, RequestQueue, ResultQueue } from '../../crawl/interfaces/queue.interface.js';

/** 큐마다 메시지 엔드포인트가 다를 수 있어 클라이언트를 따로 둠 */
function createQueueClient(
  authenticationDetailsProvider: AuthenticationDetailsProvider,
  endpoint: string
): QueueClient {
  const client = new QueueClient({ authenticationDetailsProvider });
  client.endpoint = endpoint;
  return client;
}

export class OciRequestQueue extends RequestQueue {
  private static readonly logger = new Logger(OciRequestQueue.name);

  private constructor(
    private readonly client: QueueClient,
    private readonly queueId: string
  ) {
    super();
  }

  static of(
    authenticationDetailsProvider: AuthenticationDetailsProvider,
    queueId: string,
    endpoint: string
  ): OciRequestQueue {
    return new OciRequestQueue(createQueueClient(authenticationDetailsProvider, endpoint), queueId);
  }

  override async receive(limit: number): Promise<ReceivedMessage[]> {
    const response = await this.client.getMessages({
      queueId: this.queueId,
      limit,
      visibilityInSeconds: Policy.MESSAGE_VISIBILITY_SECONDS,
      timeoutInSeconds: Policy.QUEUE_POLLING_TIMEOUT_SECONDS
    });
    return response.getMessages.messages.map(({ content, receipt, deliveryCount }) => {
      return { content, receipt, deliveryCount };
    });
  }

  override async completeAll(receipts: string[]): Promise<void> {
    if (receipts.length === 0) {
      return;
    }
    const response = await this.client.deleteMessages({
      queueId: this.queueId,
      deleteMessagesDetails: { entries: receipts.map((receipt) => ({ receipt })) }
    });
    OciRequestQueue.logFailedDeletes(response.deleteMessagesResult);
  }

  private static logFailedDeletes(result: models.DeleteMessagesResult): void {
    const failedCount = result.serverFailures + result.clientFailures;
    if (failedCount === 0) {
      return;
    }
    OciRequestQueue.logger.error(`요청 ${failedCount}건 완료 처리 실패. 같은 결과가 다시 발행될 수 있음`, {
      errors: result.entries.filter((entry) => entry.errorCode !== undefined)
    });
  }
}

export class OciResultQueue extends ResultQueue {
  private constructor(
    private readonly client: QueueClient,
    private readonly queueId: string
  ) {
    super();
  }

  static of(
    authenticationDetailsProvider: AuthenticationDetailsProvider,
    queueId: string,
    endpoint: string
  ): OciResultQueue {
    return new OciResultQueue(createQueueClient(authenticationDetailsProvider, endpoint), queueId);
  }

  override async publishAll(contents: string[]): Promise<void> {
    if (contents.length === 0) {
      return;
    }
    await this.client.putMessages({
      queueId: this.queueId,
      putMessagesDetails: { messages: contents.map((content) => ({ content })) }
    });
  }
}
