import { Logger } from '@nestjs/common';
import { bufferTime, filter, lastValueFrom, mergeMap, Subject } from 'rxjs';
import { Policy } from '../../config/policy.js';
import type { RequestQueue, ResultQueue } from '../interfaces/queue.interface.js';

type PendingResult = { resultContent: string; requestReceipt: string };

/**
 * 결과를 모아 한 번에 발행하고, 발행에 성공한 결과의 요청만 한 번에 완료 처리함
 *
 * 발행에 실패하면 요청을 완료 처리하지 않으므로 큐가 다시 전달함. 결과가 유실되지 않는 대신 중복될 수 있음
 */
export class CrawlResultBatchService {
  private readonly logger = new Logger(CrawlResultBatchService.name);
  private readonly pendingResults = new Subject<PendingResult>();
  private readonly allBatchesHandled: Promise<void>;

  constructor(
    private readonly requestQueue: RequestQueue,
    private readonly resultQueue: ResultQueue
  ) {
    this.allBatchesHandled = lastValueFrom(
      this.pendingResults.pipe(
        bufferTime(Policy.RESULT_FLUSH_DELAY_MILLIS, null, Policy.QUEUE_BATCH_SIZE),
        filter((resultBatch) => resultBatch.length > 0),
        mergeMap((resultBatch) => this.publishResultsThenCompleteRequests(resultBatch))
      ),
      { defaultValue: undefined }
    );
  }

  add(resultContent: string, requestReceipt: string): void {
    this.pendingResults.next({ resultContent, requestReceipt });
  }

  /** 남은 결과를 모두 발행하고 해당 요청을 완료 처리할 때까지 기다림 */
  async close(): Promise<void> {
    this.pendingResults.complete();
    await this.allBatchesHandled;
  }

  private async publishResultsThenCompleteRequests(resultBatch: PendingResult[]): Promise<void> {
    try {
      await this.resultQueue.publishAll(resultBatch.map(({ resultContent }) => resultContent));
    } catch (error) {
      this.logger.error(`결과 ${resultBatch.length}건 발행 실패. 요청을 다시 전달받아 처리함`, error);
      return;
    }

    try {
      await this.requestQueue.completeAll(resultBatch.map(({ requestReceipt }) => requestReceipt));
    } catch (error) {
      this.logger.error(`요청 ${resultBatch.length}건 완료 처리 실패. 같은 결과가 다시 발행될 수 있음`, error);
    }
  }
}
