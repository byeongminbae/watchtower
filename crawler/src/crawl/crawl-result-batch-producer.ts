import { Logger } from '@nestjs/common';
import { Policy } from '../config/policy.js';
import type { RequestQueue, ResultQueue } from './interfaces/queue.js';

type BatchItem = { readonly requestReceipt: string; readonly resultContent: string };

/**
 * OCI Queue 는 API 요청 횟수가 곧 비용이라, 크롤링 결과를 모아 결과 큐에 한 번에 발행하고 발행에 성공한 요청 메시지만 한 번에 완료 처리(삭제)함
 *
 * 발행에 실패하면 요청을 완료 처리하지 않으므로 큐가 다시 전달함. 결과가 유실되지 않는 대신 중복될 수 있음
 */
export class CrawlResultBatchProducer {
  private readonly logger = new Logger(CrawlResultBatchProducer.name);
  private pendingItems: BatchItem[] = [];
  private flushTimer: NodeJS.Timeout | undefined;
  private readonly inProgressPublishes = new Set<Promise<void>>();

  constructor(
    private readonly requestQueue: RequestQueue,
    private readonly resultQueue: ResultQueue
  ) {}

  add(requestReceipt: string, resultContent: string): void {
    this.pendingItems.push({ requestReceipt, resultContent });
    if (this.pendingItems.length >= Policy.QUEUE_BATCH_SIZE) {
      void this.flush();
      return;
    }
    this.flushTimer ??= setTimeout(() => void this.flush(), Policy.RESULT_FLUSH_DELAY_MILLIS);
  }

  async close(): Promise<void> {
    await this.flush();
    await Promise.all(this.inProgressPublishes);
  }

  private async flush(): Promise<void> {
    clearTimeout(this.flushTimer);
    this.flushTimer = undefined;
    if (this.pendingItems.length === 0) {
      return;
    }
    const batchItems = this.pendingItems;
    this.pendingItems = []; // await 뒤에서 클리어하면 publishWithProgressTracking 진행하는동안 들어온 메시지도 클리어됨
    await this.publishWithProgressTracking(batchItems);
  }

  /** close() 가 먼저 시작된 발행까지 기다릴 수 있도록, 발행이 끝날 때까지 진행 중 목록에 넣어둠 */
  private async publishWithProgressTracking(batchItems: BatchItem[]): Promise<void> {
    const publishing = this.publishResultsThenCompleteRequests(batchItems);
    this.inProgressPublishes.add(publishing);
    await publishing;
    this.inProgressPublishes.delete(publishing);
  }

  private async publishResultsThenCompleteRequests(batchItems: BatchItem[]): Promise<void> {
    try {
      await this.resultQueue.publishAll(batchItems.map(({ resultContent }) => resultContent));
    } catch (error) {
      this.logger.error(`결과 ${batchItems.length}건 발행 실패. 요청을 다시 전달받아 처리함`, error);
      return;
    }

    try {
      await this.requestQueue.completeAll(batchItems.map(({ requestReceipt }) => requestReceipt));
    } catch (error) {
      this.logger.error(`요청 ${batchItems.length}건 완료 처리 실패. 같은 결과가 다시 발행될 수 있음`, error);
    }
  }
}
