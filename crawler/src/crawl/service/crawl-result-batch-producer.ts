import { Logger } from '@nestjs/common';
import { bufferTime, filter, lastValueFrom, mergeMap, Subject } from 'rxjs';
import { Policy } from '../../config/policy.js';
import type { RequestQueue, ResultQueue } from '../interfaces/queue.interface.js';

type BatchItem = { resultContent: string; requestReceipt: string };

/**
 * OCI Queue 는 API 요청 횟수가 곧 비용이라, 크롤링 결과를 모아 결과 큐에 한 번에 발행하고 발행에 성공한 요청 메시지만 한 번에 완료 처리(삭제)함
 * 환경마다 결과 큐가 달라서 [EnvironmentResource] 가 환경마다 하나씩 가짐
 *
 * 발행에 실패하면 요청을 완료 처리하지 않으므로 큐가 다시 전달함. 결과가 유실되지 않는 대신 중복될 수 있음
 */
export class CrawlResultBatchProducer {
  private readonly logger = new Logger(CrawlResultBatchProducer.name);
  private readonly batchItems = new Subject<BatchItem>();
  private readonly allBatchesHandled: Promise<void>;

  constructor(
    private readonly requestQueue: RequestQueue,
    private readonly resultQueue: ResultQueue
  ) {
    this.allBatchesHandled = lastValueFrom(
      this.batchItems.pipe(
        bufferTime(Policy.RESULT_FLUSH_DELAY_MILLIS, null, Policy.QUEUE_BATCH_SIZE),
        filter((batchItems) => batchItems.length > 0),
        mergeMap((batchItems) => this.publishResultsThenCompleteRequests(batchItems))
      ),
      { defaultValue: undefined }
    );
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

  add(resultContent: string, requestReceipt: string): void {
    // Subject 에 넣기만 하고 바로 발행하지 않음. 생성자의 bufferTime 이 QUEUE_BATCH_SIZE 만큼 모이거나 RESULT_FLUSH_DELAY_MILLIS 가 지나면 모아서 발행함
    this.batchItems.next({ resultContent, requestReceipt });
  }

  /** 남은 결과를 모두 발행하고 해당 요청을 완료 처리할 때까지 기다림 */
  async close(): Promise<void> {
    this.batchItems.complete();
    await this.allBatchesHandled;
  }
}
