/** 실행 모드와 상관없이 크롤러 동작을 정하는 고정값 */
export class Policy {
  /** 페이지 하나를 여는(load 이벤트까지) 최대 시간. 넘기면 NAVIGATION_TIMEOUT 실패 결과로 돌려줌 */
  static readonly NAVIGATION_TIMEOUT_MILLIS = 30_000;
  /** 저장할 HTML 의 최대 크기. 넘기면 메모리와 저장소를 아끼기 위해 버리고 HTML_TOO_LARGE 실패 결과로 돌려줌 */
  static readonly MAX_HTML_BYTES = 5 * 1024 * 1024;
  /**
   * 받은 메시지를 다른 수신자에게 숨겨두는 시간. 이 안에 완료 처리하지 않으면 큐에 다시 나타나 다시 전달됨
   * 크롤링 도중 다시 나타나 중복 크롤링되지 않도록 탐색 제한 시간보다 넉넉하게 잡음
   */
  static readonly MESSAGE_VISIBILITY_SECONDS = 120;
  /**
   * 큐가 비어 있을 때 receive() 가 메시지를 기다리는 최대 시간(롱 폴링). 그 안에 하나라도 들어오면 바로 응답하고, 끝까지 없으면 빈 배열을 돌려줌
   * 짧으면 빈 큐에 대한 요청 수(비용)가 늘고, 길면 크롤러 종료가 그만큼 늦어짐
   */
  static readonly QUEUE_POLLING_TIMEOUT_SECONDS = 30;
  /** 큐에서 메시지를 받다가 실패했을 때 다시 받기 전에 기다리는 시간. 큐 장애 중에 요청을 쉬지 않고 보내지 않기 위함 */
  static readonly QUEUE_RETRY_DELAY_MILLIS = 5_000;
  /** OCI Queue 가 한 번의 받기, 넣기, 완료 처리 요청에 허용하는 최대 메시지 수. 요청 수가 곧 비용이라 최대한 묶음 */
  static readonly QUEUE_BATCH_SIZE = 20;
  /** 결과가 QUEUE_BATCH_SIZE 만큼 모이지 않아도 이 시간이 지나면 모인 결과를 발행함. 결과가 적을 때 발행이 늦어지지 않게 하기 위함 */
  static readonly RESULT_FLUSH_DELAY_MILLIS = 1_000;
  /** 페이지를 열 때 요청하지 않는 리소스 종류. HTML 수집에 필요 없어서 받지 않고 메모리와 대역폭을 아낌 */
  static readonly SKIPPED_RESOURCE_TYPES: ReadonlySet<string> = new Set(['image', 'media', 'font']);
  /** 같은 로그를 Slack 으로 다시 보내지 않는 시간. 큐 장애처럼 같은 오류가 몇 초마다 반복될 때 알림이 쏟아지지 않게 하기 위함 */
  static readonly SLACK_DUPLICATE_SUPPRESSION_MILLIS = 10 * 60 * 1_000;
}
