import { Injectable } from '@nestjs/common';
import {
  type Browser,
  type Page,
  type Request,
  type Response,
  type Route,
  type WebSocketRoute,
  chromium,
  errors
} from 'playwright';
import { CrawlError } from '../../crawl/dto/crawl-result-message.dto.js';
import { Policy } from '../../config/policy.js';
import {
  type PageFetcher,
  type PageFetchResult,
  PageFetchFailure,
  PageFetchSuccess
} from '../../crawl/interfaces/page-fetcher.interface.js';
import { CrawlHostValidator } from '../../crawl/validator/crawl-host.validator.js';
import { DnsResolver } from '../dns/dns-resolver.js';

/**
 * Playwright 로 URL 의 페이지를 열어 HTML 을 받아옴. 내부망 같은 허용되지 않는 주소에 접근하려 하면 HTML 을 버리고 실패로 돌려줌
 *
 * 크롤링마다 브라우저를 새로 띄워 쿠키, 저장소, 브라우저 프로세스가 다른 크롤링과 섞이지 않게 함
 * 리다이렉트로 이어진 요청은 route 핸들러를 거치지 않아서 요청 전에 막을 수 없음(직접 확인함)
 * 그래서 탐색이 끝난 뒤 리다이렉트를 거친 모든 주소를 다시 검사하고, 하나라도 허용되지 않으면 HTML 을 버림
 * 요청 자체가 내부망에 도달하는 것은 크롤러 서버의 망 격리로 막아야 함
 */
@Injectable()
export class PlaywrightPageFetcher implements PageFetcher {
  /**
   * Chromium 에 넘기는 환경변수. 이렇게 지정하지 않으면 크롤러의 환경변수 전체가 넘어가서,
   * 브라우저가 장악되면(v8 엔진 공격 등) OCI 개인키 같은 비밀값까지 읽힘
   */
  private static readonly BROWSER_ENVIRONMENT_VARIABLES = { HOME: '/tmp', PATH: '/usr/local/bin:/usr/bin:/bin' };

  constructor(private readonly dnsResolver: DnsResolver) {}

  async fetchPage(url: string): Promise<PageFetchResult> {
    const hostValidator = new CrawlHostValidator(this.dnsResolver);
    if (!(await hostValidator.isAllowedUrl(url))) {
      return new PageFetchFailure(CrawlError.URL_NOT_ALLOWED);
    }

    const browser = await chromium.launch({ env: PlaywrightPageFetcher.BROWSER_ENVIRONMENT_VARIABLES });
    try {
      const page = await PlaywrightPageFetcher.openPageBlockingDisallowedRequests(browser, hostValidator);

      let mainResponse: Response | null;
      try {
        mainResponse = await page.goto(url, { waitUntil: 'load', timeout: Policy.NAVIGATION_TIMEOUT_MILLIS });
      } catch (error) {
        // 브라우저가 죽은 경우만 일시적인 오류로 보고 예외를 던져 메시지를 다시 전달받음
        if (!browser.isConnected()) {
          throw error;
        }
        const isTimeout = error instanceof errors.TimeoutError;
        return new PageFetchFailure(isTimeout ? CrawlError.NAVIGATION_TIMEOUT : CrawlError.NAVIGATION_FAILED);
      }

      // about:blank 나 같은 문서 안의 해시 이동처럼 응답이 없는 탐색이면 null 이 옴
      if (mainResponse === null) {
        return new PageFetchFailure(CrawlError.NAVIGATION_FAILED);
      }
      if (!(await PlaywrightPageFetcher.isEveryRedirectUrlAllowed(mainResponse, hostValidator))) {
        return new PageFetchFailure(CrawlError.URL_NOT_ALLOWED);
      }

      await PlaywrightPageFetcher.waitForClientRendering(page);

      const html = await page.content();
      if (Buffer.byteLength(html) > Policy.MAX_HTML_BYTES) {
        return new PageFetchFailure(CrawlError.HTML_TOO_LARGE);
      }
      return new PageFetchSuccess({ httpStatusCode: mainResponse.status(), finalUrl: page.url(), html });
    } finally {
      await browser.close();
    }
  }

  /** 페이지가 허용되지 않는 주소로 요청을 보내거나 크롤러 서버에 파일을 남기지 못하도록 규칙을 건 브라우저 컨텍스트에서 페이지를 염 */
  private static async openPageBlockingDisallowedRequests(
    browser: Browser,
    hostValidator: CrawlHostValidator
  ): Promise<Page> {
    const browserContext = await browser.newContext({
      // 서비스 워커 등록을 막음. 서비스 워커가 보내는 요청은 아래 route 핸들러를 거치지 않아 주소 검사를 피할 수 있기 때문
      serviceWorkers: 'block',
      acceptDownloads: false
    });
    // WebSocket 은 route 핸들러를 거치지 않아서 따로 거름. CSR 페이지가 WebSocket 으로 내용을 받을 수 있어서 공인 주소면 연결함
    await browserContext.routeWebSocket(/.*/, (webSocketRoute) =>
      PlaywrightPageFetcher.connectOrCloseWebSocket(webSocketRoute, hostValidator)
    );
    // 페이지가 보내는 모든 HTTP 요청을 allowOrBlockRequest 로 거름. 수집에 필요 없는 리소스와 공인 주소가 아닌 요청을 막음
    await browserContext.route('**/*', (route) => PlaywrightPageFetcher.allowOrBlockRequest(route, hostValidator));

    const page = await browserContext.newPage();
    await PlaywrightPageFetcher.allowOnlyFirstMainFrameNavigation(page);
    return page;
  }

  /**
   * 메인 프레임 이동은 처음 한 번(goto)만 허용하고, 그 뒤 페이지 스크립트가 다른 주소로 이동하려 하면 막음
   * 스크립트가 시작한 이동의 리다이렉트도 route 핸들러를 거치지 않아서, 막지 않으면 내부 주소의 HTML 을 수집하게 될 수 있음
   * 페이지 route 가 컨텍스트 route 보다 먼저 실행되므로, 막지 않는 요청은 fallback 으로 allowOrBlockRequest 에 넘김
   */
  private static async allowOnlyFirstMainFrameNavigation(page: Page): Promise<void> {
    let isFirstMainFrameNavigation = true;
    await page.route('**/*', (route) => {
      const request = route.request();
      if (!request.isNavigationRequest() || request.frame() !== page.mainFrame()) {
        return route.fallback();
      }
      if (isFirstMainFrameNavigation) {
        isFirstMainFrameNavigation = false;
        return route.fallback();
      }
      // blockedbyclient 로 막으면 Chromium 이 오류 페이지로 바꿔서 처음 페이지를 잃음. aborted 는 이동만 취소하고 지금 문서를 유지함
      return route.abort('aborted');
    });
  }

  private static async allowOrBlockRequest(route: Route, hostValidator: CrawlHostValidator): Promise<void> {
    const request = route.request();
    if (Policy.SKIPPED_RESOURCE_TYPES.has(request.resourceType())) {
      return route.abort('blockedbyclient');
    }
    if (!(await hostValidator.isAllowedUrl(request.url()))) {
      return route.abort('blockedbyclient');
    }
    return route.continue();
  }

  private static async connectOrCloseWebSocket(
    webSocketRoute: WebSocketRoute,
    hostValidator: CrawlHostValidator
  ): Promise<void> {
    if (!(await hostValidator.isAllowedWebSocketUrl(webSocketRoute.url()))) {
      return webSocketRoute.close();
    }
    webSocketRoute.connectToServer();
  }

  /** load 이후 CSR 페이지가 데이터를 받아 그릴 때까지 기다림. 제한 시간을 넘기면 실패로 보지 않고 그때까지 그려진 HTML 을 수집함 */
  private static async waitForClientRendering(page: Page): Promise<void> {
    try {
      await page.waitForLoadState('networkidle', { timeout: Policy.CLIENT_RENDERING_WAIT_MILLIS });
    } catch (error) {
      if (!(error instanceof errors.TimeoutError)) {
        throw error;
      }
    }
  }

  /** 최종 요청부터 redirectedFrom 으로 처음 요청까지 거슬러 올라가며, 거친 주소가 모두 허용되는지 검사함 */
  private static async isEveryRedirectUrlAllowed(
    mainResponse: Response,
    hostValidator: CrawlHostValidator
  ): Promise<boolean> {
    let request: Request | null = mainResponse.request();
    while (request !== null) {
      if (!(await hostValidator.isAllowedUrl(request.url()))) {
        return false;
      }
      request = request.redirectedFrom();
    }
    return true;
  }
}
