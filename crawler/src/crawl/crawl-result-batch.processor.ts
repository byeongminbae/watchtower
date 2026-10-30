import { Logger } from '@nestjs/common';
import { bufferTime, filter, lastValueFrom, mergeMap, Subject } from 'rxjs';
import { Policy } from '../config/policy.js';
import type { RequestQueue, ResultQueue } from './interfaces/queue.interface.js';

type CrawlResultEntry = { content: string; receipt: string };

/**
 * 결과를 모아 한 번에 발행하고, 발행에 성공한 결과의 요청만 한 번에 완료 처리함
 *
 * 발행에 실패하면 요청을 완료 처리하지 않으므로 큐가 다시 전달함. 결과가 유실되지 않는 대신 중복될 수 있음
 */
export class CrawlResultBatchProcessor {
  private readonly logger = new Logger(CrawlResultBatchProcessor.name);
  private readonly entries = new Subject<CrawlResultEntry>();
  private readonly processing: Promise<void>;

  constructor(
    private readonly requestQueue: RequestQueue,
    private readonly resultQueue: ResultQueue
  ) {
    this.processing = lastValueFrom(
      this.entries.pipe(
        bufferTime(Policy.RESULT_FLUSH_DELAY_MILLIS, null, Policy.QUEUE_BATCH_SIZE),
        filter((batch) => batch.length > 0),
        mergeMap((batch) => this.publishThenComplete(batch))
      ),
      { defaultValue: undefined }
    );
  }

  add(content: string, receipt: string): void {
    this.entries.next({ content, receipt });
  }

  /** 남은 결과를 모두 발행할 때까지 기다림 */
  async close(): Promise<void> {
    this.entries.complete();
    await this.processing;
  }

  private async publishThenComplete(batch: CrawlResultEntry[]): Promise<void> {
    try {
      await this.resultQueue.publishAll(batch.map(({ content }) => content));
    } catch (error) {
      this.logger.error(`결과 ${batch.length}건 발행 실패. 요청을 다시 전달받아 처리함`, error);
      return;
    }

    try {
      await this.requestQueue.completeAll(batch.map(({ receipt }) => receipt));
    } catch (error) {
      this.logger.error(`요청 ${batch.length}건 완료 처리 실패. 같은 결과가 다시 발행될 수 있음`, error);
    }
  }
}
