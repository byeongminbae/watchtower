import { describe, expect, it } from "vitest"
import { IpAddress } from "../../src/crawl/ip-address.ts"

describe("IpAddress", () => {
  it("외부 IPv4와 IPv6 주소는 내부 주소가 아님", () => {
    for (const value of ["8.8.8.8", "2001:4860:4860::8888"]) {
      // given
      const address = IpAddress.from(value)

      // when
      const result = address?.isInternal()

      // then
      expect(result, value).toBe(false)
    }
  })

  it("내부망, 메타데이터, 멀티캐스트 주소는 내부 주소로 판단함", () => {
    for (const value of [
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
      // given
      const address = IpAddress.from(value)

      // when
      const result = address?.isInternal()

      // then
      expect(result, value).toBe(true)
    }
  })

  it("IPv6 형식으로 감싼 내부 IPv4 주소도 내부 주소로 판단함", () => {
    // given
    const address = IpAddress.from("::ffff:169.254.169.254")

    // when
    const result = address?.isInternal()

    // then
    expect(result).toBe(true)
  })

  it("IP 형식이 아니면 주소로 만들지 않음", () => {
    // given, when
    const address = IpAddress.from("not-an-ip")

    // then
    expect(address).toBeUndefined()
  })
})
