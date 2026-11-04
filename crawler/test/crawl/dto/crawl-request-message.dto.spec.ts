import { CrawlRequestMessage } from '../../../src/crawl/dto/crawl-request-message.dto.js';

describe('CrawlRequestMessage.from', () => {
  it('타워 ID, http(s) URL, 예정 시각, 요청한 회원, 직전 버전 HTML 해시가 모두 올바르면 요청으로 인정함', () => {
    // given
    const content = JSON.stringify({
      towerId: 1,
      url: 'https://example.com/a',
      scheduledAt: '2026-10-30T00:00:00.000Z',
      triggeredByMemberId: 21,
      previousHtmlSha256: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'
    });

    // when
    const request = CrawlRequestMessage.from(content);

    // then
    expect(request).toMatchObject({
      towerId: 1,
      url: 'https://example.com/a',
      scheduledAt: '2026-10-30T00:00:00.000Z',
      triggeredByMemberId: 21,
      previousHtmlSha256: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'
    });
  });

  it('직전 버전이 없어 HTML 해시가 null 인 요청도 요청으로 인정함', () => {
    // given
    const content = JSON.stringify({
      towerId: 1,
      url: 'https://example.com/a',
      scheduledAt: '2026-10-30T00:00:00.000Z',
      triggeredByMemberId: null,
      previousHtmlSha256: null
    });

    // when
    const request = CrawlRequestMessage.from(content);

    // then
    expect(request).toMatchObject({ towerId: 1, previousHtmlSha256: null });
  });

  it('요청한 회원이 null 인 자동 요청도 요청으로 인정함', () => {
    // given
    const content = JSON.stringify({
      towerId: 1,
      url: 'https://example.com/a',
      scheduledAt: '2026-10-30T00:00:00.000Z',
      triggeredByMemberId: null,
      previousHtmlSha256: null
    });

    // when
    const request = CrawlRequestMessage.from(content);

    // then
    expect(request).toMatchObject({ towerId: 1, triggeredByMemberId: null });
  });

  it('HTML 저장 위치는 같은 요청이면 항상 같음', () => {
    // given
    const content = JSON.stringify({
      towerId: 1,
      url: 'https://example.com/a',
      scheduledAt: '2026-10-30T00:00:00.000Z',
      triggeredByMemberId: null,
      previousHtmlSha256: null
    });

    // when
    const objectNames = [
      CrawlRequestMessage.from(content)?.toHtmlObjectName(),
      CrawlRequestMessage.from(content)?.toHtmlObjectName()
    ];

    // then
    expect(objectNames).toEqual(['towers/1/2026-10-30T00:00:00.000Z.html', 'towers/1/2026-10-30T00:00:00.000Z.html']);
  });

  it('http(s) 가 아닌 URL 은 백엔드에서 검증했더라도 거부함', () => {
    for (const url of ['file:///etc/passwd', 'ftp://example.com', 'javascript:alert(1)', 'data:text/html,hi']) {
      // given
      const content = JSON.stringify({
        towerId: 1,
        url,
        scheduledAt: '2026-10-30T00:00:00.000Z',
        triggeredByMemberId: null,
        previousHtmlSha256: null
      });

      // when
      const request = CrawlRequestMessage.from(content);

      // then
      expect(request, url).toBeUndefined();
    }
  });

  it('예정 시각이 UTC 의 ISO 8601 날짜와 시각이 아니면 요청으로 인정하지 않음', () => {
    for (const scheduledAt of ['1', 'Oct 30 2026', '2026/10/30 12:00', '2026-10-30', '2026-10-30T09:00:00+09:00']) {
      // given
      const content = JSON.stringify({
        towerId: 1,
        url: 'https://example.com',
        scheduledAt,
        triggeredByMemberId: null,
        previousHtmlSha256: null
      });

      // when
      const request = CrawlRequestMessage.from(content);

      // then
      expect(request, scheduledAt).toBeUndefined();
    }
  });

  it('JSON 이 아니거나 필드가 빠지거나 형식이 틀리면 요청으로 인정하지 않음', () => {
    for (const content of [
      'not json',
      'null',
      JSON.stringify({
        url: 'https://example.com',
        scheduledAt: '2026-10-30T00:00:00.000Z',
        triggeredByMemberId: null,
        previousHtmlSha256: null
      }),
      JSON.stringify({
        towerId: 0,
        url: 'https://example.com',
        scheduledAt: '2026-10-30T00:00:00.000Z',
        triggeredByMemberId: null,
        previousHtmlSha256: null
      }),
      JSON.stringify({
        towerId: '1',
        url: 'https://example.com',
        scheduledAt: '2026-10-30T00:00:00.000Z',
        triggeredByMemberId: null,
        previousHtmlSha256: null
      }),
      JSON.stringify({
        towerId: 1,
        url: 'https://example.com',
        scheduledAt: '어제',
        triggeredByMemberId: null,
        previousHtmlSha256: null
      }),
      JSON.stringify({
        towerId: 1.5,
        url: 'https://example.com',
        scheduledAt: '2026-10-30T00:00:00.000Z',
        triggeredByMemberId: null,
        previousHtmlSha256: null
      }),
      JSON.stringify({
        towerId: 1,
        url: 'https://example.com',
        scheduledAt: '2026-10-30T00:00:00.000Z',
        previousHtmlSha256: null
      }),
      JSON.stringify({
        towerId: 1,
        url: 'https://example.com',
        scheduledAt: '2026-10-30T00:00:00.000Z',
        triggeredByMemberId: '21',
        previousHtmlSha256: null
      }),
      JSON.stringify({
        towerId: 1,
        url: 'https://example.com',
        scheduledAt: '2026-10-30T00:00:00.000Z',
        triggeredByMemberId: null
      }),
      JSON.stringify({
        towerId: 1,
        url: 'https://example.com',
        scheduledAt: '2026-10-30T00:00:00.000Z',
        triggeredByMemberId: null,
        previousHtmlSha256: 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'
      }),
      JSON.stringify({
        towerId: 1,
        url: 'https://example.com',
        scheduledAt: '2026-10-30T00:00:00.000Z',
        triggeredByMemberId: null,
        previousHtmlSha256: 'abc'
      })
    ]) {
      // given, when
      const request = CrawlRequestMessage.from(content);

      // then
      expect(request, content).toBeUndefined();
    }
  });
});
