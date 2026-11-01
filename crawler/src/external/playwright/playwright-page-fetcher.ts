import { Injectable } from '@nestjs/common';
import { type Browser, type Page, type Request, type Response, type Route, chromium, errors } from 'playwright';
import { CrawlError } from '../../crawl/dto/crawl-result-message.dto.js';
import { Policy } from '../../config/policy.js';
import type { PageFetcher, PageFetchOutcome } from '../../crawl/interfaces/page-fetcher.interface.js';
import { CrawlHostValidator } from '../../crawl/validator/crawl-host.validator.js';
import { DnsResolver } from '../dns/dns-resolver.js';

/**
 * 크롤링마다 브라우저를 새로 띄워 쿠키, 저장소, 브라우저 프로세스가 다른 크롤링과 섞이지 않게 함
 *
 * 리다이렉트로 이어진 요청은 route 핸들러를 거치지 않아서 요청 전에 막을 수 없음(직접 확인함)
 * 그래서 탐색이 끝난 뒤 리다이렉트를 거친 모든 주소를 다시 검사하고, 하나라도 허용되지 않으면 HTML 을 버림
 * 요청 자체가 내부망에 도달하는 것은 크롤러 서버의 망 격리로 막아야 함
 */
@Injectable()
export class PlaywrightPageFetcher implements PageFetcher {
  /** 기본값은 크롤러의 환경변수 전체라, 브라우저가 장악되면 OCI 개인키 같은 비밀값까지 읽힘 */
  private static readonly BROWSER_ENVIRONMENT_VARIABLES = { HOME: '/tmp', PATH: '/usr/local/bin:/usr/bin:/bin' };

  constructor(private readonly dnsResolver: DnsResolver) {}

  async fetchPage(url: string): Promise<PageFetchOutcome> {
    const hostValidator = new CrawlHostValidator(this.dnsResolver);
    if (!(await hostValidator.isAllowedUrl(url))) {
      return { kind: 'failed', error: CrawlError.URL_NOT_ALLOWED };
    }

    const browser = await chromium.launch({ env: PlaywrightPageFetcher.BROWSER_ENVIRONMENT_VARIABLES });
    try {
      const page = await PlaywrightPageFetcher.openFilteredPage(browser, hostValidator);
      const mainResponse = await page
        .goto(url, { waitUntil: 'load', timeout: Policy.NAVIGATION_TIMEOUT_MILLIS })
        .catch((error: unknown) => PlaywrightPageFetcher.toNavigationCrawlError(error, browser));
      if (mainResponse instanceof CrawlError) {
        return { kind: 'failed', error: mainResponse };
      }
      return await PlaywrightPageFetcher.toFetchOutcome(page, mainResponse, hostValidator);
    } finally {
      await browser.close();
    }
  }

  private static async openFilteredPage(browser: Browser, hostValidator: CrawlHostValidator): Promise<Page> {
    const browserContext = await browser.newContext({ serviceWorkers: 'block', acceptDownloads: false });
    await browserContext.routeWebSocket(/.*/, (webSocket) => webSocket.close());
    await browserContext.route('**/*', (route) => PlaywrightPageFetcher.allowOrBlock(route, hostValidator));
    return browserContext.newPage();
  }

  private static async allowOrBlock(route: Route, hostValidator: CrawlHostValidator): Promise<void> {
    const request = route.request();
    const isAllowed =
      !Policy.SKIPPED_RESOURCE_TYPES.has(request.resourceType()) && (await hostValidator.isAllowedUrl(request.url()));
    return isAllowed ? route.continue() : route.abort('blockedbyclient');
  }

  /** 브라우저가 죽은 경우만 일시적인 오류로 보고 예외를 던져 메시지를 다시 전달받음 */
  private static toNavigationCrawlError(error: unknown, browser: Browser): CrawlError {
    if (!browser.isConnected()) {
      throw error;
    }
    return error instanceof errors.TimeoutError ? CrawlError.NAVIGATION_TIMEOUT : CrawlError.NAVIGATION_FAILED;
  }

  private static async toFetchOutcome(
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
    if (Buffer.byteLength(html) > Policy.MAX_HTML_BYTES) {
      return { kind: 'failed', error: CrawlError.HTML_TOO_LARGE };
    }
    return { kind: 'fetched', page: { httpStatusCode: mainResponse.status(), finalUrl: page.url(), html } };
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
}
