import { Injectable, type OnApplicationShutdown } from '@nestjs/common';
import {
  type Browser,
  type BrowserContext,
  type Page,
  type Request,
  type Response,
  type Route,
  chromium,
  errors
} from 'playwright';
import { CrawlError } from '../../crawl/crawl-result.js';
import { Policy } from '../../config/policy.js';
import type { PageFetcher, PageFetchOutcome } from '../../crawl/interfaces/page-fetcher.interface.js';
import { HostCheck } from '../../crawl/host-check.js';
import { DnsResolver } from '../dns/dns-resolver.js';

/**
 * 브라우저 하나를 띄워 두고 크롤링마다 새 컨텍스트를 열어 쿠키와 저장소가 섞이지 않게 함
 *
 * 리다이렉트로 이어진 요청은 route 핸들러를 거치지 않아서 요청 전에 막을 수 없음(직접 확인함)
 * 그래서 탐색이 끝난 뒤 리다이렉트를 거친 모든 주소를 다시 검사하고, 하나라도 허용되지 않으면 HTML 을 버림
 * 요청 자체가 내부망에 도달하는 것은 크롤러 서버의 망 격리로 막아야 함
 */
@Injectable()
export class PlaywrightPageFetcher implements PageFetcher, OnApplicationShutdown {
  /** 기본값은 크롤러의 환경변수 전체라, 브라우저가 장악되면 OCI 개인키 같은 비밀값까지 읽힘 */
  private static readonly BROWSER_ENV = { HOME: '/tmp', PATH: '/usr/local/bin:/usr/bin:/bin' };
  private browser: Promise<Browser> | undefined;

  constructor(private readonly dnsResolver: DnsResolver) {}

  async fetchPage(url: string): Promise<PageFetchOutcome> {
    const browser = await this.getBrowser();
    const hostCheck = new HostCheck(this.dnsResolver);
    const requestFilter = new RequestFilter(hostCheck);
    const context = await this.openContext(browser, requestFilter);

    try {
      const page = await context.newPage();
      let response: Response | null;
      try {
        response = await page.goto(url, { waitUntil: 'load', timeout: Policy.NAVIGATION_TIMEOUT_MILLIS });
      } catch (error) {
        return { kind: 'failed', error: this.toNavigationFailure(error, browser, requestFilter) };
      }
      return await this.toOutcome(page, response, hostCheck);
    } finally {
      await context.close();
    }
  }

  async onApplicationShutdown(): Promise<void> {
    const browser = this.browser;
    this.browser = undefined;
    await (await browser)?.close();
  }

  private async openContext(browser: Browser, requestFilter: RequestFilter): Promise<BrowserContext> {
    const context = await browser.newContext({ serviceWorkers: 'block', acceptDownloads: false });
    await context.routeWebSocket(/.*/, (webSocket) => webSocket.close());
    await context.route('**/*', (route) => requestFilter.handle(route));
    return context;
  }

  /** 브라우저가 죽은 경우만 일시적인 오류로 보고 예외를 던져 메시지를 다시 전달받음 */
  private toNavigationFailure(error: unknown, browser: Browser, requestFilter: RequestFilter): CrawlError {
    if (requestFilter.isMainNavigationBlocked) {
      return CrawlError.URL_NOT_ALLOWED;
    }
    if (error instanceof errors.TimeoutError) {
      return CrawlError.NAVIGATION_TIMEOUT;
    }
    if (!browser.isConnected()) {
      throw error;
    }
    return CrawlError.NAVIGATION_FAILED;
  }

  private async toOutcome(page: Page, response: Response | null, hostCheck: HostCheck): Promise<PageFetchOutcome> {
    if (response === null) {
      return { kind: 'failed', error: CrawlError.NAVIGATION_FAILED };
    }
    if (!(await PlaywrightPageFetcher.isRedirectChainAllowed(response, hostCheck))) {
      return { kind: 'failed', error: CrawlError.URL_NOT_ALLOWED };
    }

    const html = await page.content();
    if (PlaywrightPageFetcher.exceedsHtmlLimit(html)) {
      return { kind: 'failed', error: CrawlError.HTML_TOO_LARGE };
    }
    return { kind: 'fetched', page: { httpStatusCode: response.status(), finalUrl: page.url(), html } };
  }

  private getBrowser(): Promise<Browser> {
    if (this.browser === undefined) {
      const browser = chromium.launch({ env: PlaywrightPageFetcher.BROWSER_ENV });
      this.forgetWhenDisconnected(browser);
      this.browser = browser;
    }
    return this.browser;
  }

  private forgetWhenDisconnected(browser: Promise<Browser>): void {
    const forget = (): void => {
      if (this.browser === browser) {
        this.browser = undefined;
      }
    };
    browser.then((launched) => launched.on('disconnected', forget), forget);
  }

  private static async isRedirectChainAllowed(response: Response, hostCheck: HostCheck): Promise<boolean> {
    for (let request: Request | null = response.request(); request !== null; request = request.redirectedFrom()) {
      if (!(await hostCheck.isAllowedUrl(request.url()))) {
        return false;
      }
    }
    return true;
  }

  private static exceedsHtmlLimit(html: string): boolean {
    return Buffer.byteLength(html) > Policy.MAX_HTML_BYTES;
  }
}

/** 크롤링 한 건 동안 브라우저가 보내는 요청을 걸러내고, 메인 페이지 탐색이 막혔는지 기억함 */
class RequestFilter {
  private mainNavigationBlocked = false;

  constructor(private readonly hostCheck: HostCheck) {}

  get isMainNavigationBlocked(): boolean {
    return this.mainNavigationBlocked;
  }

  async handle(route: Route): Promise<void> {
    const request = route.request();
    if (RequestFilter.isSkippedResource(request)) {
      return route.abort('blockedbyclient');
    }
    if (!(await this.hostCheck.isAllowedUrl(request.url()))) {
      this.mainNavigationBlocked ||= RequestFilter.isMainFrameNavigation(request);
      return route.abort('blockedbyclient');
    }
    return route.continue();
  }

  private static isSkippedResource(request: Request): boolean {
    return Policy.SKIPPED_RESOURCE_TYPES.has(request.resourceType());
  }

  private static isMainFrameNavigation(request: Request): boolean {
    return request.isNavigationRequest() && request.frame().parentFrame() === null;
  }
}
