import { createServer, type Server } from "node:http"
import type { AddressInfo } from "node:net"
import { afterAll, beforeAll, describe, expect, it } from "vitest"
import { CrawlError } from "../../../src/crawl/crawl-error.ts"
import { CrawlPolicy } from "../../../src/crawl/crawl-policy.ts"
import { PlaywrightPageFetcher } from "../../../src/external/playwright/playwright-page-fetcher.ts"

/** 127.0.0.1 은 외부 주소, localhost 는 내부 주소로 취급해 같은 서버로 허용과 차단을 모두 재현함 */
const pageFetcher = new PlaywrightPageFetcher(() => {
  return async (host) => {
    return host === "127.0.0.1"
  }
})

let server: Server
let allowedOrigin: string
let blockedOrigin: string
let secretRequestCount = 0

beforeAll(async () => {
  server = createServer((request, response) => {
    if (request.url === "/secret") {
      secretRequestCount++
    }
    const blockedSecretUrl = `${blockedOrigin}/secret`
    response.setHeader("content-type", "text/html; charset=utf-8")
    switch (request.url) {
      case "/page":
        response.end(`<html><body>감시 대상<img src="${blockedSecretUrl}"><script src="${blockedSecretUrl}"></script></body></html>`)
        return
      case "/missing":
        response.writeHead(404).end("<html><body>없음</body></html>")
        return
      case "/redirect-to-blocked":
        response.writeHead(302, { location: blockedSecretUrl }).end()
        return
      case "/large":
        response.end(`<html><body>${"a".repeat(CrawlPolicy.MAX_HTML_BYTES)}</body></html>`)
        return
      default:
        response.end("<html><body>내부 비밀</body></html>")
    }
  })
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve))
  const { port } = server.address() as AddressInfo
  allowedOrigin = `http://127.0.0.1:${port}`
  blockedOrigin = `http://localhost:${port}`
})

afterAll(async () => {
  await pageFetcher.onApplicationShutdown()
  server.close()
})

describe("PlaywrightPageFetcher", { timeout: 30_000 }, () => {
  it("허용된 주소의 페이지를 받아오고 내부 주소를 가리키는 하위 리소스는 막음", async () => {
    // given
    const url = `${allowedOrigin}/page`
    secretRequestCount = 0

    // when
    const outcome = await pageFetcher.fetchPage(url)

    // then
    expect(outcome).toMatchObject({ kind: "fetched", page: { httpStatusCode: 200, finalUrl: url } })
    expect(outcome.kind === "fetched" && outcome.page.html).toContain("감시 대상")
    expect(secretRequestCount).toBe(0)
  })

  it("4xx 응답도 받아온 페이지로 돌려주고 판단은 백엔드에 맡김", async () => {
    // given
    const url = `${allowedOrigin}/missing`

    // when
    const outcome = await pageFetcher.fetchPage(url)

    // then
    expect(outcome).toMatchObject({ kind: "fetched", page: { httpStatusCode: 404 } })
  })

  it("내부 주소로 바로 접근하면 허용되지 않은 주소로 실패함", async () => {
    // given
    const url = `${blockedOrigin}/secret`

    // when
    const outcome = await pageFetcher.fetchPage(url)

    // then
    expect(outcome).toEqual({ kind: "failed", error: CrawlError.URL_NOT_ALLOWED })
  })

  it("외부 주소가 내부 주소로 리다이렉트하면 받아온 내부 페이지를 버리고 허용되지 않은 주소로 실패함", async () => {
    // given
    const url = `${allowedOrigin}/redirect-to-blocked`

    // when
    const outcome = await pageFetcher.fetchPage(url)

    // then
    expect(outcome).toEqual({ kind: "failed", error: CrawlError.URL_NOT_ALLOWED })
  })

  it("HTML 이 수집 한도를 넘으면 크기 초과로 실패함", async () => {
    // given
    const url = `${allowedOrigin}/large`

    // when
    const outcome = await pageFetcher.fetchPage(url)

    // then
    expect(outcome).toEqual({ kind: "failed", error: CrawlError.HTML_TOO_LARGE })
  })

  it("연결할 수 없는 주소는 탐색 실패로 돌려줌", async () => {
    // given
    const closedServer = createServer()
    await new Promise<void>((resolve) => closedServer.listen(0, "127.0.0.1", resolve))
    const { port } = closedServer.address() as AddressInfo
    await new Promise<void>((resolve) => closedServer.close(() => resolve()))

    // when
    const outcome = await pageFetcher.fetchPage(`http://127.0.0.1:${port}/`)

    // then
    expect(outcome).toEqual({ kind: "failed", error: CrawlError.NAVIGATION_FAILED })
  })
})
