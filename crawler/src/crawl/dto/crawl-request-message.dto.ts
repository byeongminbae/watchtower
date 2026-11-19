import { z } from 'zod';
import { WebUrl } from '../../network/web-url.js';
import type { ContentHashes } from '../vo/content-hashes.js';

const SHA256_HEX = z.string().regex(/^[0-9a-f]{64}$/);

const CRAWL_REQUEST_SCHEMA = z.object({
  towerId: z.int().positive(),
  url: z.string().refine((value) => WebUrl.from(value)?.isHttp() === true),
  extraction: z.object({
    detectionAreaSelector: z.string().min(1).nullable(),
    collectImageHashes: z.boolean(),
    ignoreDateTime: z.boolean()
  }),
  triggeredByMemberId: z.int().positive().nullable(),
  previousTextHash: SHA256_HEX.nullable(),
  previousImageHash: SHA256_HEX.nullable(),
  jobStartedAt: z.iso.datetime({ local: true })
});

type CrawlRequest = z.infer<typeof CRAWL_REQUEST_SCHEMA>;

export type Extraction = Readonly<CrawlRequest['extraction']>;

/**
 * 백엔드 스케줄러가 요청 큐에 넣는 크롤링 요청. 모르는 필드는 무시함
 *
 * @property url 백엔드에서 검증했더라도 크롤러는 신뢰하지 않고 다시 검사해야함
 */
export class CrawlRequestMessage implements Readonly<CrawlRequest> {
  declare readonly towerId: number;
  declare readonly url: string;
  declare readonly extraction: Extraction;
  declare readonly triggeredByMemberId: number | null;
  declare readonly previousTextHash: string | null;
  declare readonly previousImageHash: string | null;
  declare readonly jobStartedAt: string;

  private constructor(request: CrawlRequest) {
    Object.assign(this, request);
  }

  static from(content: string): CrawlRequestMessage | null {
    const parseResult = CRAWL_REQUEST_SCHEMA.safeParse(CrawlRequestMessage.parseJson(content));
    if (!parseResult.success) {
      return null;
    }
    return new CrawlRequestMessage(parseResult.data);
  }

  private static parseJson(content: string): unknown {
    try {
      return JSON.parse(content);
    } catch {
      return null;
    }
  }

  hasSameContentAsPrevious(contentHashes: ContentHashes): boolean {
    return (
      contentHashes.selectTextHash(this.extraction.ignoreDateTime) === this.previousTextHash &&
      contentHashes.imageHash === this.previousImageHash
    );
  }

  toTextObjectName(crawlingStartedAt: Date): string {
    return `towers/${this.towerId}/${crawlingStartedAt.toISOString()}.txt`;
  }
}
