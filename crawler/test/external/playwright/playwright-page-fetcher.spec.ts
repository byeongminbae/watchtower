import { createServer, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { WebSocketServer } from 'ws';
import { CrawlError } from '../../../src/crawl/dto/crawl-result-message.dto.js';
import { Policy } from '../../../src/config/policy.js';
import { PageFetchFailure } from '../../../src/crawl/interfaces/page-fetcher.interface.js';
import { DnsResolver } from '../../../src/external/dns/dns-resolver.js';
import { PlaywrightPageFetcher } from '../../../src/external/playwright/playwright-page-fetcher.js';

/** 127.0.0.1 은 외부 주소로 조회되게 하고 localhost 는 그대로 두어, 같은 테스트 서버로 허용과 차단을 모두 재현함 */
class TestDnsResolver extends DnsResolver {
  override async resolveAll(host: string): Promise<string[]> {
    return host === '127.0.0.1' ? ['93.184.216.34'] : ['127.0.0.1'];
  }
}

const pageFetcher = new PlaywrightPageFetcher(new TestDnsResolver());

let server: Server;
let allowedOrigin: string;
let blockedOrigin: string;
let secretRequestCount = 0;
let webSocketConnectionCount = 0;

beforeAll(async () => {
  server = createServer((request, response) => {
    if (request.url === '/secret') {
      secretRequestCount++;
    }
    const blockedSecretUrl = `${blockedOrigin}/secret`;
    response.setHeader('content-type', 'text/html; charset=utf-8');
    switch (request.url) {
      case '/page':
        response.end(
          `<html><body>감시 대상<img src="${blockedSecretUrl}"><script src="${blockedSecretUrl}"></script></body></html>`
        );
        return;
      case '/client-rendered':
        response.end(
          `<html><body><div id="content"></div><script>window.addEventListener('load', async () => { document.getElementById('content').textContent = await (await fetch('/client-rendered-data')).text(); });</script></body></html>`
        );
        return;
      case '/client-rendered-data':
        setTimeout(() => response.end('나중에 그려진 감시 대상'), 500);
        return;
      case '/navigate-to-blocked-after-load':
        response.end(
          `<html><body>처음 페이지<script>window.addEventListener('load', () => { location.href = '/redirect-to-blocked'; });</script></body></html>`
        );
        return;
      case '/web-socket-allowed':
        response.end(webSocketPage(allowedOrigin));
        return;
      case '/web-socket-blocked':
        response.end(webSocketPage(blockedOrigin));
        return;
      case '/missing':
        response.writeHead(404).end('<html><body>없음</body></html>');
        return;
      case '/redirect-to-blocked':
        response.writeHead(302, { location: blockedSecretUrl }).end();
        return;
      case '/large':
        response.end(`<html><body>${'a'.repeat(Policy.MAX_HTML_BYTES)}</body></html>`);
        return;
      default:
        response.end('<html><body>내부 비밀</body></html>');
    }
  });
  const webSocketServer = new WebSocketServer({ server });
  webSocketServer.on('connection', (webSocket) => {
    webSocketConnectionCount++;
    webSocket.send('WebSocket 으로 받은 감시 대상');
  });
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  const { port } = server.address() as AddressInfo;
  allowedOrigin = `http://127.0.0.1:${port}`;
  blockedOrigin = `http://localhost:${port}`;
});

/** 받은 WebSocket 메시지를 본문에 그리는 페이지. origin 의 http 를 ws 로 바꿔 연결함 */
function webSocketPage(origin: string): string {
  const webSocketUrl = origin.replace('http:', 'ws:');
  return `<html><body>처음 페이지<div id="content"></div><script>new WebSocket('${webSocketUrl}').onmessage = (event) => { document.getElementById('content').textContent = event.data; };</script></body></html>`;
}

afterAll(() => {
  server.close();
});

describe('PlaywrightPageFetcher', { timeout: 30_000 }, () => {
  it('허용된 주소의 페이지를 받아오고 내부 주소를 가리키는 하위 리소스는 막음', async () => {
    // given
    const url = `${allowedOrigin}/page`;
    secretRequestCount = 0;

    // when
    const outcome = await pageFetcher.fetchPage(url);

    // then
    expect(outcome).toMatchObject({
      page: { httpStatusCode: 200, finalUrl: url, html: expect.stringContaining('감시 대상') }
    });
    expect(secretRequestCount).toBe(0);
  });

  it('load 이후 스크립트가 받아와 그린 내용도 함께 수집함', async () => {
    // given
    const url = `${allowedOrigin}/client-rendered`;

    // when
    const result = await pageFetcher.fetchPage(url);

    // then
    expect(result).toMatchObject({ page: { html: expect.stringContaining('나중에 그려진 감시 대상') } });
  });

  it('load 이후 스크립트가 내부 주소로 리다이렉트되는 주소로 이동하려 하면 막고 처음 페이지를 수집함', async () => {
    // given
    const url = `${allowedOrigin}/navigate-to-blocked-after-load`;
    secretRequestCount = 0;

    // when
    const result = await pageFetcher.fetchPage(url);

    // then
    expect(result).toMatchObject({ page: { finalUrl: url, html: expect.stringContaining('처음 페이지') } });
    expect(result).not.toMatchObject({ page: { html: expect.stringContaining('내부 비밀') } });
    expect(secretRequestCount).toBe(0);
  });

  it('페이지가 공인 주소의 WebSocket 으로 받아 그린 내용도 함께 수집함', async () => {
    // given
    const url = `${allowedOrigin}/web-socket-allowed`;

    // when
    const result = await pageFetcher.fetchPage(url);

    // then
    expect(result).toMatchObject({ page: { html: expect.stringContaining('WebSocket 으로 받은 감시 대상') } });
  });

  it('페이지가 내부 주소로 WebSocket 을 연결하려 하면 서버에 연결하지 않고 끊음', async () => {
    // given
    const url = `${allowedOrigin}/web-socket-blocked`;
    webSocketConnectionCount = 0;

    // when
    const result = await pageFetcher.fetchPage(url);

    // then
    expect(result).toMatchObject({ page: { html: expect.stringContaining('처음 페이지') } });
    expect(result).not.toMatchObject({ page: { html: expect.stringContaining('WebSocket 으로 받은 감시 대상') } });
    expect(webSocketConnectionCount).toBe(0);
  });

  it('4xx 응답도 받아온 페이지로 돌려주고 판단은 백엔드에 맡김', async () => {
    // given
    const url = `${allowedOrigin}/missing`;

    // when
    const outcome = await pageFetcher.fetchPage(url);

    // then
    expect(outcome).toMatchObject({ page: { httpStatusCode: 404 } });
  });

  it('내부 주소로 바로 접근하면 허용되지 않은 주소로 실패함', async () => {
    // given
    const url = `${blockedOrigin}/secret`;

    // when
    const outcome = await pageFetcher.fetchPage(url);

    // then
    expect(outcome).toEqual(new PageFetchFailure(CrawlError.URL_NOT_ALLOWED));
  });

  it('외부 주소가 내부 주소로 리다이렉트하면 받아온 내부 페이지를 버리고 허용되지 않은 주소로 실패함', async () => {
    // given
    const url = `${allowedOrigin}/redirect-to-blocked`;

    // when
    const outcome = await pageFetcher.fetchPage(url);

    // then
    expect(outcome).toEqual(new PageFetchFailure(CrawlError.URL_NOT_ALLOWED));
  });

  it('HTML 이 수집 한도를 넘으면 크기 초과로 실패함', async () => {
    // given
    const url = `${allowedOrigin}/large`;

    // when
    const outcome = await pageFetcher.fetchPage(url);

    // then
    expect(outcome).toEqual(new PageFetchFailure(CrawlError.HTML_TOO_LARGE));
  });

  it('연결할 수 없는 주소는 탐색 실패로 돌려줌', async () => {
    // given
    const closedServer = createServer();
    await new Promise<void>((resolve) => closedServer.listen(0, '127.0.0.1', resolve));
    const { port } = closedServer.address() as AddressInfo;
    await new Promise<void>((resolve) => closedServer.close(() => resolve()));

    // when
    const outcome = await pageFetcher.fetchPage(`http://127.0.0.1:${port}/`);

    // then
    expect(outcome).toEqual(new PageFetchFailure(CrawlError.NAVIGATION_FAILED));
  });
});
