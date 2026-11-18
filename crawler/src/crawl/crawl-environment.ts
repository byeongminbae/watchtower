import type { DeploymentEnvironment } from '../config/profile.js';
import { CrawlResultBatchProducer } from './crawl-result-batch-producer.js';
import type { CrawlResultMessage } from './dto/crawl-result-message.dto.js';
import type { RequestQueue, ResultQueue } from './interfaces/queue.js';
import type { SnapshotStorage } from './interfaces/snapshot-storage.js';

/**
 * 크롤러는 dev, prod 요청을 한 프로세스에서 처리하지만 결과가 섞이면 안되기 때문에 환경마다 하나씩 만들어짐
 *
 * 환경은 메시지 내용이 아니라 요청을 받은 큐로 정함. 메시지에 환경을 적게 하면 dev 의 실수가 prod 타워에 반영될 수 있음
 */
export class CrawlEnvironment {
  private readonly resultBatchProducer: CrawlResultBatchProducer;

  constructor(
    readonly deploymentEnvironment: DeploymentEnvironment,
    readonly requestQueue: RequestQueue,
    resultQueue: ResultQueue,
    readonly snapshotStorage: SnapshotStorage
  ) {
    this.resultBatchProducer = new CrawlResultBatchProducer(requestQueue, resultQueue);
  }

  publishResult(requestReceipt: string, result: CrawlResultMessage): void {
    this.resultBatchProducer.add(requestReceipt, JSON.stringify(result));
  }

  async publishRemainingResults(): Promise<void> {
    await this.resultBatchProducer.close();
  }
}

export class CrawlEnvironments {
  constructor(readonly all: readonly CrawlEnvironment[]) {}

  async publishRemainingResults(): Promise<void> {
    await Promise.all(this.all.map((environment) => environment.publishRemainingResults()));
  }
}
