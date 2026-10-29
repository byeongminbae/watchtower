export class CrawlPolicy {
  static readonly NAVIGATION_TIMEOUT_MILLIS = 30_000;
  static readonly MAX_HTML_BYTES = 5 * 1024 * 1024;
  /** 크롤링 한 건이 끝나기 전에 다른 소비자에게 다시 보이지 않도록 탐색 제한 시간보다 넉넉하게 잡음 */
  static readonly MESSAGE_VISIBILITY_SECONDS = 120;
  static readonly QUEUE_POLLING_TIMEOUT_SECONDS = 30;
  static readonly QUEUE_RETRY_DELAY_MILLIS = 5_000;
  /** OCI Queue 가 한 번의 넣기, 받기, 삭제 요청에 허용하는 최대 메시지 수. 요청 수가 곧 비용이라 최대한 묶음 */
  static readonly QUEUE_BATCH_SIZE = 20;
  /** 결과가 배치 크기만큼 모이지 않아도 이 시간이 지나면 발행함 */
  static readonly RESULT_FLUSH_DELAY_MILLIS = 1_000;
  /** HTML 수집에 필요 없는 리소스는 받지 않아 메모리와 대역폭을 아낌 */
  static readonly SKIPPED_RESOURCE_TYPES: ReadonlySet<string> = new Set(['image', 'media', 'font']);
}
