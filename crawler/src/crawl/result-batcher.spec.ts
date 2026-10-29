import type { RequestQueue, ResultQueue } from './interfaces/queue.interface.js';
import { CrawlPolicy } from './crawl-policy.js';
import { ResultBatcher } from './result-batcher.js';

describe('ResultBatcher', () => {
  let resultQueue: ResultQueue;
  let requestQueue: RequestQueue;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.spyOn(console, 'error').mockImplementation(() => {});
    resultQueue = { publishAll: vi.fn(async () => {}) };
    requestQueue = { receive: vi.fn(async () => []), completeAll: vi.fn(async () => {}) };
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('배치 크기만큼 모이면 기다리지 않고 한 번에 발행한 뒤 한 번에 완료 처리함', async () => {
    // given
    const batcher = new ResultBatcher(resultQueue, requestQueue);

    // when
    for (let index = 0; index < CrawlPolicy.QUEUE_BATCH_SIZE; index++) {
      batcher.add(`result-${index}`, `receipt-${index}`);
    }
    await vi.waitFor(() => expect(requestQueue.completeAll).toHaveBeenCalled());

    // then
    expect(resultQueue.publishAll).toHaveBeenCalledTimes(1);
    expect(vi.mocked(resultQueue.publishAll).mock.calls[0]?.[0]).toHaveLength(CrawlPolicy.QUEUE_BATCH_SIZE);
    expect(vi.mocked(requestQueue.completeAll).mock.calls[0]?.[0]).toHaveLength(CrawlPolicy.QUEUE_BATCH_SIZE);
  });

  it('배치 크기에 못 미쳐도 대기 시간이 지나면 모인 만큼 발행함', async () => {
    // given
    const batcher = new ResultBatcher(resultQueue, requestQueue);
    batcher.add('result-1', 'receipt-1');

    // when
    await vi.advanceTimersByTimeAsync(CrawlPolicy.RESULT_FLUSH_DELAY_MILLIS);

    // then
    expect(resultQueue.publishAll).toHaveBeenCalledWith(['result-1']);
    expect(requestQueue.completeAll).toHaveBeenCalledWith(['receipt-1']);
  });

  it('발행에 실패하면 요청을 완료 처리하지 않아 다시 전달받게 함', async () => {
    // given
    resultQueue.publishAll = vi.fn(async () => {
      throw new Error('결과 큐 장애');
    });
    const batcher = new ResultBatcher(resultQueue, requestQueue);
    batcher.add('result-1', 'receipt-1');

    // when
    await batcher.close();

    // then
    expect(requestQueue.completeAll).not.toHaveBeenCalled();
  });

  it('종료할 때 대기 시간 전이라도 남은 결과를 모두 발행함', async () => {
    // given
    const batcher = new ResultBatcher(resultQueue, requestQueue);
    batcher.add('result-1', 'receipt-1');

    // when
    await batcher.close();

    // then
    expect(resultQueue.publishAll).toHaveBeenCalledWith(['result-1']);
    expect(requestQueue.completeAll).toHaveBeenCalledWith(['receipt-1']);
  });
});
