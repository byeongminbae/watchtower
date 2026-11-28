import type { Browser, Page, Request, Response, Route, WebSocketRoute } from 'playwright';
import { Policy } from '../../config/policy.js';
import type { CrawlHostValidator } from '../../network/crawl-host-validator.js';

/**
 * 리다이렉트로 이어진 요청은 route 핸들러를 거치지 않아서 요청 전에 막을 수 없음(직접 확인함)
 * 그래서 탐색이 끝난 뒤 리다이렉트를 거친 모든 주소를 다시 검사하고, 하나라도 허용되지 않으면 텍스트를 버림
 * 요청 자체가 내부망에 도달하는 것은 크롤러 서버의 망 격리로 막아야 함
 */
export class RequestFilter {
  constructor(private readonly hostValidator: CrawlHostValidator) {}

  /**
   * 메인 프레임 이동은 처음 한 번(goto)만 허용하고, 그 뒤 페이지 스크립트가 다른 주소로 이동하려 하면 막음
   * 스크립트가 시작한 이동의 리다이렉트도 route 핸들러를 거치지 않아서, 막지 않으면 내부 주소의 내용을 수집하게 될 수 있음
   * 페이지 route 가 컨텍스트 route 보다 먼저 실행되므로, 막지 않는 요청은 fallback 으로 allowOrBlockRequest 에 넘김
   */
  private static async allowOnlyFirstMainFrameNavigation(page: Page): Promise<void> {
    let isFirstMainFrameNavigation = true;
    // 콜백은 페이지가 보내는 요청마다 불림
    await page.route('**/*', (route) => {
      const request = route.request();
      // 이미지, 스크립트, XHR 같은 하위 리소스나 iframe 안의 이동은 넘기고
      // goto, location.href 변경, 링크 클릭, 폼 제출 같은 메인 프레임 이동만 아래에서 판정함
      if (!request.isNavigationRequest() || request.frame() !== page.mainFrame()) {
        return route.fallback();
      }
      if (isFirstMainFrameNavigation) {
        isFirstMainFrameNavigation = false;
        return route.fallback();
      }
      // blockedbyclient 로 막으면 Chromium 이 오류 페이지로 바꿔서 처음 페이지를 잃음. aborted 는 이동만 취소하고 지금 페이지를 유지함
      return route.abort('aborted');
    });
  }

  private static toRedirectChainUrls(finalRequest: Request): string[] {
    const urls: string[] = [];
    let request: Request | null = finalRequest;
    while (request !== null) {
      urls.push(request.url());
      request = request.redirectedFrom();
    }
    return urls;
  }

  async openFilteredPage(browser: Browser): Promise<Page> {
    const browserContext = await browser.newContext({
      // 서비스 워커 등록을 막음. 서비스 워커가 보내는 요청은 아래 route 핸들러를 거치지 않아 주소 검사를 피할 수 있기 때문
      serviceWorkers: 'block',
      acceptDownloads: false
    });
    // WebSocket 은 route 핸들러를 거치지 않아서 따로 거름. CSR 페이지가 WebSocket 으로 내용을 받을 수 있어서 공인 주소면 연결함
    await browserContext.routeWebSocket(/.*/, (webSocketRoute) => this.connectOrCloseWebSocket(webSocketRoute));
    await browserContext.route('**/*', (route) => this.allowOrBlockRequest(route));

    const page = await browserContext.newPage();
    await RequestFilter.allowOnlyFirstMainFrameNavigation(page);
    return page;
  }

  async isEveryRedirectUrlAllowed(mainResponse: Response): Promise<boolean> {
    const redirectChainUrls = RequestFilter.toRedirectChainUrls(mainResponse.request());
    const allowedResults = await Promise.all(redirectChainUrls.map((url) => this.hostValidator.isAllowedUrl(url)));
    return allowedResults.every((isAllowed) => isAllowed);
  }

  private async allowOrBlockRequest(route: Route): Promise<void> {
    const request = route.request();
    if (Policy.SKIPPED_RESOURCE_TYPES.has(request.resourceType())) {
      return route.abort('blockedbyclient');
    }
    if (!(await this.hostValidator.isAllowedUrl(request.url()))) {
      return route.abort('blockedbyclient');
    }
    return route.continue();
  }

  private async connectOrCloseWebSocket(webSocketRoute: WebSocketRoute): Promise<void> {
    if (!(await this.hostValidator.isAllowedWebSocketUrl(webSocketRoute.url()))) {
      return webSocketRoute.close();
    }
    webSocketRoute.connectToServer();
  }
}
