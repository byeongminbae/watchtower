import { z } from 'zod';
import { HttpUrl } from '../validator/crawl-host.validator.js';

/**
 * 백엔드 스케줄러가 요청 큐에 넣는 크롤링 요청
 *
 * @property towerId 크롤링할 타워
 * @property url 백엔드에서 검증했더라도 크롤러는 이 값을 신뢰하지 않고 다시 검사함
 * @property scheduledAt 백엔드가 정한 실행 예정 시각(ISO 8601, UTC). towerId 와 함께 결과의 중복 반영을 막는 키로 쓰임
 */
export class CrawlRequestMessage {
  private static readonly SCHEMA = z.object({
    towerId: z.int().positive(),
    url: z.string().refine((value) => HttpUrl.from(value) !== undefined),
    scheduledAt: z.iso.datetime()
  });

  private constructor(
    readonly towerId: number,
    readonly url: string,
    readonly scheduledAt: string
  ) {}

  /** @return 형식이 맞지 않거나 http(s) 가 아닌 URL 이면 undefined */
  static from(content: string): CrawlRequestMessage | undefined {
    const parsed = CrawlRequestMessage.SCHEMA.safeParse(CrawlRequestMessage.parseJson(content));
    if (!parsed.success) {
      return undefined;
    }

    const { towerId, url, scheduledAt } = parsed.data;
    return new CrawlRequestMessage(towerId, url, scheduledAt);
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
}
