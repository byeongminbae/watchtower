import { describe, expect, it, vi } from "vitest"
import { HostCheck } from "../../src/crawl/host-check.ts"
import { DnsResolver } from "../../src/external/dns/dns-resolver.ts"

function createDnsResolver(records: Record<string, string[]>): DnsResolver {
  const dnsResolver = new DnsResolver()
  vi.spyOn(dnsResolver, "resolveAll").mockImplementation(async (host) => records[host] ?? [])
  return dnsResolver
}

describe("HostCheck", () => {
  it("조회된 주소가 모두 외부 주소일 때만 접근을 허용함", async () => {
    // given
    const hostCheck = new HostCheck(createDnsResolver({
      "example.com": ["93.184.216.34", "2606:2800:220:1::1"],
      "mixed.example": ["8.8.8.8", "10.0.0.1"]
    }))

    // when
    const results = await Promise.all([hostCheck.isAllowedHost("example.com"), hostCheck.isAllowedHost("mixed.example")])

    // then
    expect(results).toEqual([true, false])
  })

  it("조회 결과가 없으면 접근을 허용하지 않음", async () => {
    // given
    const hostCheck = new HostCheck(createDnsResolver({}))

    // when
    const result = await hostCheck.isAllowedHost("unknown.example")

    // then
    expect(result).toBe(false)
  })

  it("한 번의 크롤링 안에서 같은 호스트는 한 번만 조회함", async () => {
    // given
    const dnsResolver = createDnsResolver({ "example.com": ["8.8.8.8"] })
    const hostCheck = new HostCheck(dnsResolver)

    // when
    await Promise.all([hostCheck.isAllowedHost("example.com"), hostCheck.isAllowedHost("example.com")])

    // then
    expect(dnsResolver.resolveAll).toHaveBeenCalledTimes(1)
  })

  it("대괄호로 감싼 IPv6 호스트는 대괄호를 벗겨서 조회함", async () => {
    // given
    const dnsResolver = createDnsResolver({ "::1": ["::1"] })
    const hostCheck = new HostCheck(dnsResolver)

    // when
    const result = await hostCheck.isAllowedHost("[::1]")

    // then
    expect(dnsResolver.resolveAll).toHaveBeenCalledWith("::1")
    expect(result).toBe(false)
  })

  it("http(s) 가 아닌 주소는 호스트를 조회하지 않고 거부함", async () => {
    // given
    const dnsResolver = createDnsResolver({ "example.com": ["8.8.8.8"] })
    const hostCheck = new HostCheck(dnsResolver)

    // when
    const result = await hostCheck.isAllowedUrl("ftp://example.com/file")

    // then
    expect(result).toBe(false)
    expect(dnsResolver.resolveAll).not.toHaveBeenCalled()
  })
})
