import { CrawlRequestMessage } from './crawl-request-message.dto.js';

describe('CrawlRequest.from', () => {
  it('타워 ID, http(s) URL, 예정 시각이 모두 올바르면 요청으로 인정함', () => {
    // given
    const content = JSON.stringify({
      towerId: 1,
      url: 'https://example.com/a',
      scheduledAt: '2026-10-30T00:00:00.000Z'
    });

    // when
    const request = CrawlRequestMessage.from(content);

    // then
    expect(request).toMatchObject({
      towerId: 1,
      url: 'https://example.com/a',
      scheduledAt: '2026-10-30T00:00:00.000Z'
    });
  });

  it('HTML 저장 위치는 같은 요청이면 항상 같음', () => {
    // given
    const content = JSON.stringify({
      towerId: 1,
      url: 'https://example.com/a',
      scheduledAt: '2026-10-30T00:00:00.000Z'
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
      const content = JSON.stringify({ towerId: 1, url, scheduledAt: '2026-10-30T00:00:00.000Z' });

      // when
      const request = CrawlRequestMessage.from(content);

      // then
      expect(request, url).toBeUndefined();
    }
  });

  it('예정 시각이 UTC 의 ISO 8601 날짜와 시각이 아니면 요청으로 인정하지 않음', () => {
    for (const scheduledAt of ['1', 'Oct 30 2026', '2026/10/30 12:00', '2026-10-30', '2026-10-30T09:00:00+09:00']) {
      // given
      const content = JSON.stringify({ towerId: 1, url: 'https://example.com', scheduledAt });

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
      JSON.stringify({ url: 'https://example.com', scheduledAt: '2026-10-30T00:00:00.000Z' }),
      JSON.stringify({ towerId: 0, url: 'https://example.com', scheduledAt: '2026-10-30T00:00:00.000Z' }),
      JSON.stringify({ towerId: '1', url: 'https://example.com', scheduledAt: '2026-10-30T00:00:00.000Z' }),
      JSON.stringify({ towerId: 1, url: 'https://example.com', scheduledAt: '어제' }),
      JSON.stringify({ towerId: 1.5, url: 'https://example.com', scheduledAt: '2026-10-30T00:00:00.000Z' })
    ]) {
      // given, when
      const request = CrawlRequestMessage.from(content);

      // then
      expect(request, content).toBeUndefined();
    }
  });
});
