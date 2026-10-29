import { HttpUrl } from './http-url.js';

/**
 * 백엔드 스케줄러가 요청 큐에 넣는 크롤링 요청
 *
 * @property towerId 크롤링할 타워
 * @property url 백엔드에서 검증했더라도 크롤러는 이 값을 신뢰하지 않고 다시 검사함
 * @property scheduledAt 백엔드가 정한 실행 예정 시각(ISO 8601, UTC). towerId 와 함께 결과의 중복 반영을 막는 키로 쓰임
 */
export class CrawlRequest {
  private constructor(
    readonly towerId: number,
    readonly url: string,
    readonly scheduledAt: string
  ) {}

  /** @return 형식이 맞지 않거나 http(s) 가 아닌 URL 이면 undefined */
  static from(content: string): CrawlRequest | undefined {
    const message = CrawlRequest.parseJson(content);
    if (!CrawlRequest.isObject(message)) {
      return undefined;
    }

    const { towerId, url, scheduledAt } = message;
    if (
      !CrawlRequest.isPositiveInteger(towerId) ||
      !CrawlRequest.isHttpUrl(url) ||
      !CrawlRequest.isDateTime(scheduledAt)
    ) {
      return undefined;
    }
    return new CrawlRequest(towerId, url, scheduledAt);
  }

  /** 같은 요청이 다시 처리되면 같은 객체를 덮어써서 저장소에 중복이 쌓이지 않게 함 */
  toHtmlObjectName(): string {
    return `towers/${this.towerId}/${this.scheduledAt}.html`;
  }

  private static parseJson(content: string): unknown {
    try {
      return JSON.parse(content);
    } catch {
      return undefined;
    }
  }

  private static isObject(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
  }

  private static isPositiveInteger(value: unknown): value is number {
    return typeof value === 'number' && Number.isSafeInteger(value) && value > 0;
  }

  private static isHttpUrl(value: unknown): value is string {
    return typeof value === 'string' && HttpUrl.from(value) !== undefined;
  }

  private static isDateTime(value: unknown): value is string {
    return typeof value === 'string' && !Number.isNaN(Date.parse(value));
  }
}
