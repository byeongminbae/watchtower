import { hash } from 'node:crypto';
import { createServer, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { WebSocketServer } from 'ws';
import { Policy } from '../../../src/config/policy.js';
import type { Extraction } from '../../../src/crawl/dto/crawl-request-message.dto.js';
import { FetchedPage, PageFetchFailure } from '../../../src/crawl/interfaces/page-fetcher.js';
import { PlaywrightPageFetcher } from '../../../src/external/playwright/playwright-page-fetcher.js';
import { DnsResolver } from '../../../src/network/dns-resolver.js';

/** 127.0.0.1 은 외부 주소로 조회되게 하고 localhost 는 그대로 두어, 같은 테스트 서버로 허용과 차단을 모두 재현함 */
class TestDnsResolver extends DnsResolver {
  override async resolveAll(host: string): Promise<string[]> {
    return host === '127.0.0.1' ? ['93.184.216.34'] : ['127.0.0.1'];
  }
}

const pageFetcher = new PlaywrightPageFetcher(new TestDnsResolver());
const wholePage: Extraction = { detectionAreaSelector: null, collectImageHashes: false, ignoreDateTime: false };
const IMAGE_BODIES: Record<string, Buffer> = {
  '/image-a.png': Buffer.from('이미지 A'),
  '/image-b.png': Buffer.from('이미지 B'),
  '/image-c.png': Buffer.from('이미지 C')
};

let server: Server;
let allowedOrigin: string;
let blockedOrigin: string;
let secretRequestCount = 0;
let webSocketConnectionCount = 0;
const requestedPaths = new Set<string>();

beforeAll(async () => {
  server = createServer((request, response) => {
    requestedPaths.add(request.url ?? '');
    if (request.url === '/secret') {
      secretRequestCount++;
    }
    const blockedSecretUrl = `${blockedOrigin}/secret`;
    const imageBody = IMAGE_BODIES[request.url ?? ''];
    if (imageBody !== undefined) {
      response.setHeader('content-type', 'image/png');
      response.end(imageBody);
      return;
    }
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
      case '/areas':
        response.end(
          `<html><body><p class="notice">공지 1</p><p>무관한 글</p><div class="notice">공지 2 <span class="notice">중첩</span></div></body></html>`
        );
        return;
      case '/images':
        response.end(
          `<html><body><div class="area"><img src="/image-a.png"><img loading="lazy" style="display:block; margin-top:5000px" src="/image-b.png"></div><img src="/image-c.png"><img src="${blockedSecretUrl}"></body></html>`
        );
        return;
      case '/hidden-elements':
        response.end(
          `<html><head><style>.hidden { display: none; }</style></head><body><p data-ved="요청마다 바뀌는 값">보이는 공지</p><p class="hidden">숨긴 공지</p><input type="hidden" value="숨긴 입력값"><script>window.trackingValue = '스크립트 값';</script></body></html>`
        );
        return;
      case '/redirect-to-blocked':
        response.writeHead(302, { location: blockedSecretUrl }).end();
        return;
      case '/redirect-to-page':
        response.writeHead(302, { location: '/page' }).end();
        return;
      case '/screenshot':
        response.end(
          `<html><body style="margin:0"><div style="height:2000px">긴 페이지</div><p class="price" style="width:120px; height:30px; margin:0">12,000원</p><p class="sold-out" style="display:none">품절</p></body></html>`
        );
        return;
      case '/rendered-resources':
        response.end(
          `<html><head><style>@font-face { font-family: Rendered; src: url(/rendered-font.woff2); } body { font-family: Rendered; }</style></head><body>웹 글꼴<img src="/rendered-image.png"><img loading="lazy" style="display:block; margin-top:5000px" src="/rendered-lazy-image.png"></body></html>`
        );
        return;
      case '/too-tall':
        response.end(
          `<html><body style="margin:0"><div style="height:${Policy.MAX_SCREENSHOT_HEIGHT_PIXELS + 500}px">아주 긴 페이지</div></body></html>`
        );
        return;
      case '/large':
        response.end(`<html><body>${'a'.repeat(Policy.MAX_TEXT_BYTES + 1)}</body></html>`);
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

function pngSize(png: Buffer): { width: number; height: number } {
  return { width: png.readUInt32BE(16), height: png.readUInt32BE(20) };
}

describe('PlaywrightPageFetcher', { timeout: 30_000 }, () => {
  it('허용된 주소의 페이지를 받아오고 내부 주소를 가리키는 하위 리소스는 막음', async () => {
    // given
    const url = `${allowedOrigin}/page`;
    secretRequestCount = 0;

    // when
    const outcome = await pageFetcher.fetchPage(url, wholePage);

    // then
    expect(outcome).toMatchObject({
      finalUrl: url,
      httpStatusCode: 200,
      text: expect.stringContaining('감시 대상'),
      imageHashes: null
    });
    expect(secretRequestCount).toBe(0);
  });

  it('감시 영역을 고르면 그 영역의 텍스트만 문서 순서대로 수집하고 영역 안에 겹친 영역은 한 번만 담음', async () => {
    // given
    const url = `${allowedOrigin}/areas`;

    // when
    const outcome = await pageFetcher.fetchPage(url, { ...wholePage, detectionAreaSelector: '.notice' });

    // then
    expect(outcome).toMatchObject({ text: '공지 1\n공지 2 중첩' });
  });

  it('영역 선택자 문법이 틀리면 선택자 오류로 실패함', async () => {
    // given
    const url = `${allowedOrigin}/areas`;

    // when
    const outcome = await pageFetcher.fetchPage(url, { ...wholePage, detectionAreaSelector: '>>>' });

    // then
    expect(outcome).toEqual(new PageFetchFailure('INVALID_SELECTOR'));
  });

  it('이미지를 수집하면 지연 로딩 이미지까지 받아 img 요소 순서대로 해시를 담고 내부 주소 이미지는 막음', async () => {
    // given
    const url = `${allowedOrigin}/images`;
    secretRequestCount = 0;

    // when
    const outcome = await pageFetcher.fetchPage(url, { ...wholePage, collectImageHashes: true });

    // then
    expect(outcome).toMatchObject({
      imageHashes: [
        hash('sha256', IMAGE_BODIES['/image-a.png']!),
        hash('sha256', IMAGE_BODIES['/image-b.png']!),
        hash('sha256', IMAGE_BODIES['/image-c.png']!)
      ]
    });
    expect(secretRequestCount).toBe(0);
  });

  it('감시 영역을 고르고 이미지를 수집하면 영역 안 이미지의 해시만 담음', async () => {
    // given
    const url = `${allowedOrigin}/images`;

    // when
    const outcome = await pageFetcher.fetchPage(url, {
      ...wholePage,
      detectionAreaSelector: '.area',
      collectImageHashes: true
    });

    // then
    expect(outcome).toMatchObject({
      imageHashes: [hash('sha256', IMAGE_BODIES['/image-a.png']!), hash('sha256', IMAGE_BODIES['/image-b.png']!)]
    });
  });

  it('화면에 보이는 텍스트만 수집하고 숨긴 요소, 속성 값, 스크립트는 수집하지 않음', async () => {
    // given
    const url = `${allowedOrigin}/hidden-elements`;

    // when
    const outcome = await pageFetcher.fetchPage(url, wholePage);

    // then
    expect(outcome).toMatchObject({ text: '보이는 공지' });
  });

  it('load 이후 스크립트가 받아와 그린 내용도 함께 수집함', async () => {
    // given
    const url = `${allowedOrigin}/client-rendered`;

    // when
    const result = await pageFetcher.fetchPage(url, wholePage);

    // then
    expect(result).toMatchObject({ text: expect.stringContaining('나중에 그려진 감시 대상') });
  });

  it('load 이후 스크립트가 내부 주소로 리다이렉트되는 주소로 이동하려 하면 막고 처음 페이지를 수집함', async () => {
    // given
    const url = `${allowedOrigin}/navigate-to-blocked-after-load`;
    secretRequestCount = 0;

    // when
    const result = await pageFetcher.fetchPage(url, wholePage);

    // then
    expect(result).toMatchObject({ text: expect.stringContaining('처음 페이지') });
    expect(result).not.toMatchObject({ text: expect.stringContaining('내부 비밀') });
    expect(secretRequestCount).toBe(0);
  });

  it('페이지가 공인 주소의 WebSocket 으로 받아 그린 내용도 함께 수집함', async () => {
    // given
    const url = `${allowedOrigin}/web-socket-allowed`;

    // when
    const result = await pageFetcher.fetchPage(url, wholePage);

    // then
    expect(result).toMatchObject({ text: expect.stringContaining('WebSocket 으로 받은 감시 대상') });
  });

  it('페이지가 내부 주소로 WebSocket 을 연결하려 하면 서버에 연결하지 않고 끊음', async () => {
    // given
    const url = `${allowedOrigin}/web-socket-blocked`;
    webSocketConnectionCount = 0;

    // when
    const result = await pageFetcher.fetchPage(url, wholePage);

    // then
    expect(result).toMatchObject({ text: expect.stringContaining('처음 페이지') });
    expect(result).not.toMatchObject({ text: expect.stringContaining('WebSocket 으로 받은 감시 대상') });
    expect(webSocketConnectionCount).toBe(0);
  });

  it('4xx 응답도 받아온 페이지로 돌려주고 판단은 백엔드에 맡김', async () => {
    // given
    const url = `${allowedOrigin}/missing`;

    // when
    const outcome = await pageFetcher.fetchPage(url, wholePage);

    // then
    expect(outcome).toMatchObject({ httpStatusCode: 404 });
  });

  it('내부 주소로 바로 접근하면 허용되지 않은 주소로 실패함', async () => {
    // given
    const url = `${blockedOrigin}/secret`;

    // when
    const outcome = await pageFetcher.fetchPage(url, wholePage);

    // then
    expect(outcome).toEqual(new PageFetchFailure('URL_NOT_ALLOWED'));
  });

  it('허용된 주소로 리다이렉트하면 리다이렉트를 따라간 뒤 페이지를 응답한 URL 을 최종 URL 로 담음', async () => {
    // given
    const url = `${allowedOrigin}/redirect-to-page`;

    // when
    const outcome = await pageFetcher.fetchPage(url, wholePage);

    // then
    expect(outcome).toMatchObject({ finalUrl: `${allowedOrigin}/page`, httpStatusCode: 200 });
  });

  it('외부 주소가 내부 주소로 리다이렉트하면 받아온 내부 페이지를 버리고 허용되지 않은 주소로 실패함', async () => {
    // given
    const url = `${allowedOrigin}/redirect-to-blocked`;

    // when
    const outcome = await pageFetcher.fetchPage(url, wholePage);

    // then
    expect(outcome).toEqual(new PageFetchFailure('URL_NOT_ALLOWED'));
  });

  it('감시 영역이 없으면 페이지 전체를 PNG 스크린샷으로 찍음', async () => {
    // given
    const url = `${allowedOrigin}/screenshot`;

    // when
    const outcome = await pageFetcher.fetchPage(url, wholePage);

    // then
    expect(outcome).toBeInstanceOf(FetchedPage);
    expect(pngSize((outcome as FetchedPage).screenshot!)).toEqual({ width: 1280, height: 2030 });
  });

  it('감시 영역을 고르면 그 영역만 스크린샷으로 찍음', async () => {
    // given
    const url = `${allowedOrigin}/screenshot`;

    // when
    const outcome = await pageFetcher.fetchPage(url, { ...wholePage, detectionAreaSelector: '.price' });

    // then
    expect(pngSize((outcome as FetchedPage).screenshot!)).toEqual({ width: 120, height: 30 });
  });

  it('감시 영역이 화면에 보이지 않으면 스크린샷을 찍지 않음', async () => {
    // given
    const url = `${allowedOrigin}/screenshot`;

    // when
    const outcome = await pageFetcher.fetchPage(url, { ...wholePage, detectionAreaSelector: '.sold-out' });

    // then
    expect(outcome).toMatchObject({ screenshot: null });
  });

  it('페이지가 스크린샷 최대 높이보다 길면 최대 높이까지만 찍음', async () => {
    // given
    const url = `${allowedOrigin}/too-tall`;

    // when
    const outcome = await pageFetcher.fetchPage(url, wholePage);

    // then
    expect(pngSize((outcome as FetchedPage).screenshot!).height).toBe(Policy.MAX_SCREENSHOT_HEIGHT_PIXELS);
  });

  it('이미지를 수집하지 않아도 스크린샷에 그릴 이미지와 지연 로딩 이미지와 웹 글꼴을 받음', async () => {
    // given
    const url = `${allowedOrigin}/rendered-resources`;

    // when
    await pageFetcher.fetchPage(url, wholePage);

    // then
    expect([...requestedPaths]).toEqual(
      expect.arrayContaining(['/rendered-image.png', '/rendered-lazy-image.png', '/rendered-font.woff2'])
    );
  });

  it('페이지 텍스트가 수집 한도를 넘으면 크기 초과로 실패함', async () => {
    // given
    const url = `${allowedOrigin}/large`;

    // when
    const outcome = await pageFetcher.fetchPage(url, wholePage);

    // then
    expect(outcome).toEqual(new PageFetchFailure('TEXT_TOO_LARGE'));
  });

  it('연결할 수 없는 주소는 탐색 실패로 돌려줌', async () => {
    // given
    const closedServer = createServer();
    await new Promise<void>((resolve) => closedServer.listen(0, '127.0.0.1', resolve));
    const { port } = closedServer.address() as AddressInfo;
    await new Promise<void>((resolve) => closedServer.close(() => resolve()));

    // when
    const outcome = await pageFetcher.fetchPage(`http://127.0.0.1:${port}/`, wholePage);

    // then
    expect(outcome).toEqual(new PageFetchFailure('NAVIGATION_FAILED'));
  });
});
