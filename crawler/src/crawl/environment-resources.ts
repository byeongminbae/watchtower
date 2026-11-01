import type { DeploymentEnvironment } from '../config/config.js';
import type { RequestQueue, ResultQueue } from './interfaces/queue.interface.js';
import { CrawlResultBatchService } from './service/crawl-result-batch.service.js';

/**
 * 크롤러는 dev, prod 요청을 한 프로세스에서 처리하지만 결과가 섞이면 안되기 때문에 [EnvironmentResource] 가 환경마다 하나씩 만들어짐
 *
 * 환경은 메시지 내용이 아니라 요청을 받은 큐로 정함. 메시지에 환경을 적게 하면 dev 의 실수가 prod 타워에 반영될 수 있음
 */
export class EnvironmentResource {
  readonly resultBatch: CrawlResultBatchService;

  constructor(
    readonly deploymentEnvironment: DeploymentEnvironment,
    readonly requestQueue: RequestQueue,
    readonly resultQueue: ResultQueue,
    readonly bucketName: string
  ) {
    this.resultBatch = new CrawlResultBatchService(requestQueue, resultQueue);
  }
}

export class EnvironmentResources {
  constructor(readonly all: EnvironmentResource[]) {}
}
