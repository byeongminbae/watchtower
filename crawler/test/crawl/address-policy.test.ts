import { describe, expect, it, vi } from "vitest"
import { createHostGuard, isInternalAddress } from "../../src/crawl/address-policy.ts"

describe("isInternalAddress", () => {
  it("외부 IPv4와 IPv6 주소는 내부 주소가 아님", () => {
    for (const address of ["8.8.8.8", "2001:4860:4860::8888"]) {
      // given, when
      const result = isInternalAddress(address)

      // then
      expect(result, address).toBe(false)
    }
  })

  it("내부망, 메타데이터, 멀티캐스트 주소는 내부 주소로 판단함", () => {
    for (const address of [
      "0.0.0.0",
      "127.0.0.1",
      "10.0.0.163",
      "100.64.0.1",
      "172.16.0.1",
      "192.168.0.1",
      "169.254.169.254",
      "224.0.0.1",
      "::",
      "::1",
      "fc00::1",
      "fd12:3456::1",
      "fe80::1",
      "fec0::1",
      "ff02::1"
    ]) {
      // given, when
      const result = isInternalAddress(address)

      // then
      expect(result, address).toBe(true)
    }
  })

  it("IPv6 형식으로 감싼 내부 IPv4 주소도 내부 주소로 판단함", () => {
    // given
    const address = "::ffff:169.254.169.254"

    // when
    const result = isInternalAddress(address)

    // then
    expect(result).toBe(true)
  })

  it("IP 형식이 아니면 판단할 수 없으므로 내부 주소로 취급함", () => {
    // given
    const address = "not-an-ip"

    // when
    const result = isInternalAddress(address)

    // then
    expect(result).toBe(true)
  })
})

describe("createHostGuard", () => {
  it("조회된 주소가 모두 외부 주소일 때만 접근을 허용함", async () => {
    // given
    const isAllowedHost = createHostGuard(async (host) => {
      return host === "example.com" ? ["93.184.216.34", "2606:2800:220:1::1"] : ["8.8.8.8", "10.0.0.1"]
    })

    // when
    const results = await Promise.all([isAllowedHost("example.com"), isAllowedHost("mixed.example")])

    // then
    expect(results).toEqual([true, false])
  })

  it("DNS 조회에 실패하거나 결과가 없으면 접근을 허용하지 않음", async () => {
    // given
    const isAllowedHost = createHostGuard(async (host) => {
      if (host === "fail.example") {
        throw new Error("ENOTFOUND")
      }
      return []
    })

    // when
    const results = await Promise.all([isAllowedHost("fail.example"), isAllowedHost("empty.example")])

    // then
    expect(results).toEqual([false, false])
  })

  it("한 번의 크롤링 안에서 같은 호스트는 한 번만 조회함", async () => {
    // given
    const lookupAll = vi.fn(async () => ["8.8.8.8"])
    const isAllowedHost = createHostGuard(lookupAll)

    // when
    await Promise.all([isAllowedHost("example.com"), isAllowedHost("example.com")])

    // then
    expect(lookupAll).toHaveBeenCalledTimes(1)
  })

  it("대괄호로 감싼 IPv6 호스트는 대괄호를 벗겨서 조회함", async () => {
    // given
    const lookupAll = vi.fn(async (host: string) => [host])
    const isAllowedHost = createHostGuard(lookupAll)

    // when
    const result = await isAllowedHost("[::1]")

    // then
    expect(lookupAll).toHaveBeenCalledWith("::1")
    expect(result).toBe(false)
  })
})
