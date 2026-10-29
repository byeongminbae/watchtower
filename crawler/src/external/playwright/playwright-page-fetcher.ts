import { Injectable, type OnApplicationShutdown } from "@nestjs/common"
import { type Browser, type Request, type Response, chromium, errors } from "playwright"
import { CrawlError } from "../../crawl/crawl-error.ts"
import { CrawlPolicy } from "../../crawl/crawl-policy.ts"
import type { PageFetcher, PageFetchOutcome } from "../../crawl/crawl.service.ts"
import { HostCheck } from "../../crawl/host-check.ts"
import { DnsResolver } from "../dns/dns-resolver.ts"

/**
 * 브라우저 하나를 띄워 두고 크롤링마다 새 컨텍스트를 열어 쿠키와 저장소가 섞이지 않게 함
 *
 * 리다이렉트로 이어진 요청은 route 핸들러를 거치지 않아서 요청 전에 막을 수 없음(직접 확인함)
 * 그래서 탐색이 끝난 뒤 리다이렉트를 거친 모든 주소를 다시 검사하고, 하나라도 허용되지 않으면 HTML 을 버림
 * 요청 자체가 내부망에 도달하는 것은 크롤러 서버의 망 격리로 막아야 함
 */
@Injectable()
export class PlaywrightPageFetcher implements PageFetcher, OnApplicationShutdown {
  private browser: Promise<Browser> | undefined

  constructor(private readonly dnsResolver: DnsResolver) {}

  async fetchPage(url: string): Promise<PageFetchOutcome> {
    const browser = await this.getBrowser()
    const hostCheck = new HostCheck(this.dnsResolver)
    const context = await browser.newContext({ serviceWorkers: "block", acceptDownloads: false })

    try {
      let isMainNavigationBlocked = false
      await context.routeWebSocket(/.*/, (webSocket) => webSocket.close())
      await context.route("**/*", async (route) => {
        const request = route.request()
        if (CrawlPolicy.SKIPPED_RESOURCE_TYPES.has(request.resourceType())) {
          return route.abort("blockedbyclient")
        }
        if (!(await hostCheck.isAllowedUrl(request.url()))) {
          isMainNavigationBlocked ||= request.isNavigationRequest() && request.frame().parentFrame() === null
          return route.abort("blockedbyclient")
        }
        return route.continue()
      })

      const page = await context.newPage()
      let response: Response | null
      try {
        response = await page.goto(url, { waitUntil: "load", timeout: CrawlPolicy.NAVIGATION_TIMEOUT_MILLIS })
      } catch (error) {
        if (isMainNavigationBlocked) {
          return { kind: "failed", error: CrawlError.URL_NOT_ALLOWED }
        }
        if (error instanceof errors.TimeoutError) {
          return { kind: "failed", error: CrawlError.NAVIGATION_TIMEOUT }
        }
        if (!browser.isConnected()) {
          throw error
        }
        return { kind: "failed", error: CrawlError.NAVIGATION_FAILED }
      }

      if (response === null) {
        return { kind: "failed", error: CrawlError.NAVIGATION_FAILED }
      }
      if (!(await this.isRedirectChainAllowed(response, hostCheck))) {
        return { kind: "failed", error: CrawlError.URL_NOT_ALLOWED }
      }

      const html = await page.content()
      if (Buffer.byteLength(html) > CrawlPolicy.MAX_HTML_BYTES) {
        return { kind: "failed", error: CrawlError.HTML_TOO_LARGE }
      }

      return {
        kind: "fetched",
        page: { httpStatusCode: response.status(), finalUrl: page.url(), html }
      }
    } finally {
      await context.close()
    }
  }

  async onApplicationShutdown(): Promise<void> {
    const browser = this.browser
    this.browser = undefined
    await (await browser)?.close()
  }

  /** 브라우저가 죽었으면 다음 크롤링에서 새로 띄움 */
  private getBrowser(): Promise<Browser> {
    if (this.browser === undefined) {
      const browser = chromium.launch()
      browser.then(
        (launched) => launched.on("disconnected", () => {
          if (this.browser === browser) {
            this.browser = undefined
          }
        }),
        () => {
          this.browser = undefined
        }
      )
      this.browser = browser
    }
    return this.browser
  }

  private async isRedirectChainAllowed(response: Response, hostCheck: HostCheck): Promise<boolean> {
    for (let request: Request | null = response.request(); request !== null; request = request.redirectedFrom()) {
      if (!(await hostCheck.isAllowedUrl(request.url()))) {
        return false
      }
    }
    return true
  }
}
