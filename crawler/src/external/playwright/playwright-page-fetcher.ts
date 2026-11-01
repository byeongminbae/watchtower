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
import { CrawlError } from '../../crawl/dto/crawl-result-message.dto.js';
import { Policy } from '../../config/policy.js';
import type { PageFetcher, PageFetchOutcome } from '../../crawl/interfaces/page-fetcher.interface.js';
import { CrawlHostValidator } from '../../crawl/validator/crawl-host.validator.js';
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
  private static readonly BROWSER_ENVIRONMENT_VARIABLES = { HOME: '/tmp', PATH: '/usr/local/bin:/usr/bin:/bin' };
  private sharedBrowser: Promise<Browser> | undefined;

  constructor(private readonly dnsResolver: DnsResolver) {}

  async fetchPage(url: string): Promise<PageFetchOutcome> {
    const browser = await this.getOrLaunchBrowser();
    const hostValidator = new CrawlHostValidator(this.dnsResolver);
    const requestFilter = new RequestFilter(hostValidator);
    const browserContext = await this.openIsolatedContext(browser, requestFilter);

    try {
      const page = await browserContext.newPage();
      let mainResponse: Response | null;
      try {
        mainResponse = await page.goto(url, { waitUntil: 'load', timeout: Policy.NAVIGATION_TIMEOUT_MILLIS });
      } catch (error) {
        return { kind: 'failed', error: this.toNavigationCrawlError(error, browser, requestFilter) };
      }
      return await this.toFetchOutcome(page, mainResponse, hostValidator);
    } finally {
      await browserContext.close();
    }
  }

  async onApplicationShutdown(): Promise<void> {
    const launchingBrowser = this.sharedBrowser;
    this.sharedBrowser = undefined;
    await (await launchingBrowser)?.close();
  }

  private async openIsolatedContext(browser: Browser, requestFilter: RequestFilter): Promise<BrowserContext> {
    const browserContext = await browser.newContext({ serviceWorkers: 'block', acceptDownloads: false });
    await browserContext.routeWebSocket(/.*/, (webSocket) => webSocket.close());
    await browserContext.route('**/*', (route) => requestFilter.allowOrBlock(route));
    return browserContext;
  }

  /** 브라우저가 죽은 경우만 일시적인 오류로 보고 예외를 던져 메시지를 다시 전달받음 */
  private toNavigationCrawlError(error: unknown, browser: Browser, requestFilter: RequestFilter): CrawlError {
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

  private async toFetchOutcome(
    page: Page,
    mainResponse: Response | null,
    hostValidator: CrawlHostValidator
  ): Promise<PageFetchOutcome> {
    if (mainResponse === null) {
      return { kind: 'failed', error: CrawlError.NAVIGATION_FAILED };
    }
    if (!(await PlaywrightPageFetcher.isEveryRedirectUrlAllowed(mainResponse, hostValidator))) {
      return { kind: 'failed', error: CrawlError.URL_NOT_ALLOWED };
    }

    const html = await page.content();
    if (PlaywrightPageFetcher.exceedsMaxHtmlBytes(html)) {
      return { kind: 'failed', error: CrawlError.HTML_TOO_LARGE };
    }
    return { kind: 'fetched', page: { httpStatusCode: mainResponse.status(), finalUrl: page.url(), html } };
  }

  private getOrLaunchBrowser(): Promise<Browser> {
    if (this.sharedBrowser === undefined) {
      const launchingBrowser = chromium.launch({ env: PlaywrightPageFetcher.BROWSER_ENVIRONMENT_VARIABLES });
      this.clearBrowserWhenDisconnected(launchingBrowser);
      this.sharedBrowser = launchingBrowser;
    }
    return this.sharedBrowser;
  }

  private clearBrowserWhenDisconnected(launchingBrowser: Promise<Browser>): void {
    const clearBrowser = (): void => {
      if (this.sharedBrowser === launchingBrowser) {
        this.sharedBrowser = undefined;
      }
    };
    launchingBrowser.then((launchedBrowser) => launchedBrowser.on('disconnected', clearBrowser), clearBrowser);
  }

  private static async isEveryRedirectUrlAllowed(
    mainResponse: Response,
    hostValidator: CrawlHostValidator
  ): Promise<boolean> {
    for (
      let redirectedRequest: Request | null = mainResponse.request();
      redirectedRequest !== null;
      redirectedRequest = redirectedRequest.redirectedFrom()
    ) {
      if (!(await hostValidator.isAllowedUrl(redirectedRequest.url()))) {
        return false;
      }
    }
    return true;
  }

  private static exceedsMaxHtmlBytes(html: string): boolean {
    return Buffer.byteLength(html) > Policy.MAX_HTML_BYTES;
  }
}

/** 크롤링 한 건 동안 브라우저가 보내는 요청을 걸러내고, 메인 페이지 탐색이 막혔는지 기억함 */
class RequestFilter {
  private mainNavigationBlocked = false;

  constructor(private readonly hostValidator: CrawlHostValidator) {}

  get isMainNavigationBlocked(): boolean {
    return this.mainNavigationBlocked;
  }

  async allowOrBlock(route: Route): Promise<void> {
    const request = route.request();
    if (RequestFilter.isSkippedResourceType(request)) {
      return route.abort('blockedbyclient');
    }
    if (!(await this.hostValidator.isAllowedUrl(request.url()))) {
      this.mainNavigationBlocked ||= RequestFilter.isMainFrameNavigation(request);
      return route.abort('blockedbyclient');
    }
    return route.continue();
  }

  private static isSkippedResourceType(request: Request): boolean {
    return Policy.SKIPPED_RESOURCE_TYPES.has(request.resourceType());
  }

  private static isMainFrameNavigation(request: Request): boolean {
    return request.isNavigationRequest() && request.frame().parentFrame() === null;
  }
}
