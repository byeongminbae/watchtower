import { CrawlRequestMessage } from '../../../src/crawl/dto/crawl-request-message.dto.js';
import { ContentHashes } from '../../../src/crawl/vo/content-hashes.js';

const TEXT_HASH = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';
const IMAGE_HASH = 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb';
const JOB_STARTED_AT = '2026-10-30T00:00:00.123456Z';
const extraction = { detectionAreaSelector: '.announcements', collectImageHashes: true, ignoreDateTime: true };

function requestContent(overrides: Record<string, unknown>): string {
  return JSON.stringify({
    towerId: 1,
    url: 'https://example.com/a',
    extraction,
    triggeredByMemberId: 21,
    previousTextHash: TEXT_HASH,
    previousImageHash: IMAGE_HASH,
    jobStartedAt: JOB_STARTED_AT,
    ...overrides
  });
}

describe('CrawlRequestMessage.from', () => {
  it('타워 ID, http(s) URL, 추출 대상, 요청한 회원, 직전 버전의 해시, 작업 시작 시각이 모두 올바르면 요청으로 인정함', () => {
    // given
    const content = requestContent({});

    // when
    const request = CrawlRequestMessage.from(content);

    // then
    expect(request).toMatchObject({
      towerId: 1,
      url: 'https://example.com/a',
      extraction,
      triggeredByMemberId: 21,
      previousTextHash: TEXT_HASH,
      previousImageHash: IMAGE_HASH,
      jobStartedAt: JOB_STARTED_AT
    });
  });

  it('직전 버전이 없어 해시가 비어 있고 영역을 고르지 않은 요청도 요청으로 인정함', () => {
    // given
    const content = requestContent({
      extraction: { detectionAreaSelector: null, collectImageHashes: false, ignoreDateTime: false },
      previousTextHash: null,
      previousImageHash: null
    });

    // when
    const request = CrawlRequestMessage.from(content);

    // then
    expect(request).toMatchObject({
      towerId: 1,
      extraction: { detectionAreaSelector: null },
      previousTextHash: null,
      previousImageHash: null
    });
  });

  it('요청한 회원이 null 인 자동 요청도 요청으로 인정함', () => {
    // given
    const content = requestContent({ triggeredByMemberId: null });

    // when
    const request = CrawlRequestMessage.from(content);

    // then
    expect(request).toMatchObject({ towerId: 1, triggeredByMemberId: null });
  });

  it('모르는 필드가 있어도 무시하고 요청으로 인정함', () => {
    // given
    const content = requestContent({ addedLater: 'value' });

    // when
    const request = CrawlRequestMessage.from(content);

    // then
    expect(request).toMatchObject({ towerId: 1 });
    expect(request).not.toHaveProperty('addedLater');
  });

  it('텍스트 저장 위치는 같은 텍스트라도 크롤링 시작 시각마다 다름', () => {
    // given
    const request = CrawlRequestMessage.from(requestContent({}))!;

    // when
    const objectNames = [
      request.toTextObjectName(new Date('2026-10-30T00:00:01.000Z')),
      request.toTextObjectName(new Date('2026-10-30T00:10:01.000Z'))
    ];

    // then
    expect(objectNames).toEqual(['towers/1/2026-10-30T00:00:01.000Z.txt', 'towers/1/2026-10-30T00:10:01.000Z.txt']);
  });

  it('날짜와 시간을 무시하면 날짜와 시간을 뺀 해시로 텍스트와 이미지가 모두 같아야 같은 내용으로 판단함', () => {
    // given
    const request = CrawlRequestMessage.from(requestContent({}))!;

    // when
    const results = [
      request.hasSameContentAsPrevious(new ContentHashes('d'.repeat(64), TEXT_HASH, IMAGE_HASH)),
      request.hasSameContentAsPrevious(new ContentHashes('d'.repeat(64), 'f'.repeat(64), IMAGE_HASH)),
      request.hasSameContentAsPrevious(new ContentHashes('d'.repeat(64), TEXT_HASH, null))
    ];

    // then
    expect(results).toEqual([true, false, false]);
  });

  it('날짜와 시간을 무시하지 않으면 원문 해시로 비교함', () => {
    // given
    const request = CrawlRequestMessage.from(requestContent({ extraction: { ...extraction, ignoreDateTime: false } }))!;
    const contentHashes = new ContentHashes(TEXT_HASH, 'd'.repeat(64), IMAGE_HASH);

    // when
    const result = request.hasSameContentAsPrevious(contentHashes);

    // then
    expect(result).toBe(true);
  });

  it('http(s) 가 아닌 URL 은 백엔드에서 검증했더라도 거부함', () => {
    for (const url of ['file:///etc/passwd', 'ftp://example.com', 'javascript:alert(1)', 'data:text/html,hi']) {
      // given
      const content = requestContent({ url });

      // when
      const request = CrawlRequestMessage.from(content);

      // then
      expect(request, url).toBeNull();
    }
  });

  it('JSON 이 아니거나 필드가 빠지거나 형식이 틀리면 요청으로 인정하지 않음', () => {
    for (const content of [
      'not json',
      'null',
      requestContent({ towerId: undefined }),
      requestContent({ towerId: 0 }),
      requestContent({ towerId: '1' }),
      requestContent({ towerId: 1.5 }),
      requestContent({ extraction: undefined }),
      requestContent({ extraction: { collectImageHashes: true, ignoreDateTime: true } }),
      requestContent({ extraction: { detectionAreaSelector: '', collectImageHashes: true, ignoreDateTime: true } }),
      requestContent({ extraction: { collectImageHashes: true, ignoreDateTime: true } }),
      requestContent({ extraction: { detectionAreaSelector: null, collectImageHashes: 'true', ignoreDateTime: true } }),
      requestContent({ extraction: { detectionAreaSelector: null, collectImageHashes: true } }),
      requestContent({ triggeredByMemberId: undefined }),
      requestContent({ triggeredByMemberId: '21' }),
      requestContent({ previousTextHash: undefined }),
      requestContent({ previousTextHash: TEXT_HASH.toUpperCase() }),
      requestContent({ previousTextHash: 'abc' }),
      requestContent({ previousImageHash: undefined }),
      requestContent({ previousImageHash: IMAGE_HASH.toUpperCase() }),
      requestContent({ previousImageHash: 'abc' }),
      requestContent({ jobStartedAt: undefined }),
      requestContent({ jobStartedAt: '2026-10-30' }),
      requestContent({ jobStartedAt: 'not datetime' })
    ]) {
      // given, when
      const request = CrawlRequestMessage.from(content);

      // then
      expect(request, content).toBeNull();
    }
  });
});
