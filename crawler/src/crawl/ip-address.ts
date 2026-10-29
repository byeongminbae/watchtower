import { BlockList, isIP } from "node:net"

export class IpAddress {
  private static readonly INTERNAL_RANGES = new BlockList()

  static {
    IpAddress.INTERNAL_RANGES.addSubnet("0.0.0.0", 8, "ipv4")
    IpAddress.INTERNAL_RANGES.addSubnet("10.0.0.0", 8, "ipv4")
    IpAddress.INTERNAL_RANGES.addSubnet("100.64.0.0", 10, "ipv4")
    IpAddress.INTERNAL_RANGES.addSubnet("127.0.0.0", 8, "ipv4")
    IpAddress.INTERNAL_RANGES.addSubnet("169.254.0.0", 16, "ipv4")
    IpAddress.INTERNAL_RANGES.addSubnet("172.16.0.0", 12, "ipv4")
    IpAddress.INTERNAL_RANGES.addSubnet("192.168.0.0", 16, "ipv4")
    IpAddress.INTERNAL_RANGES.addSubnet("224.0.0.0", 4, "ipv4")
    IpAddress.INTERNAL_RANGES.addAddress("::", "ipv6")
    IpAddress.INTERNAL_RANGES.addAddress("::1", "ipv6")
    IpAddress.INTERNAL_RANGES.addSubnet("fc00::", 7, "ipv6")
    IpAddress.INTERNAL_RANGES.addSubnet("fe80::", 10, "ipv6")
    IpAddress.INTERNAL_RANGES.addSubnet("fec0::", 10, "ipv6")
    IpAddress.INTERNAL_RANGES.addSubnet("ff00::", 8, "ipv6")
  }

  private constructor(
    private readonly value: string,
    private readonly family: "ipv4" | "ipv6"
  ) {}

  /**
   * ::ffff:127.0.0.1 처럼 IPv6 형식으로 감싼 IPv4 주소는 IPv4 로 꺼내서 판단함
   * @return IP 형식이 아니면 undefined
   */
  static from(value: string): IpAddress | undefined {
    const mappedPrefix = "::ffff:"
    if (value.toLowerCase().startsWith(mappedPrefix) && isIP(value.slice(mappedPrefix.length)) === 4) {
      return new IpAddress(value.slice(mappedPrefix.length), "ipv4")
    }

    const version = isIP(value)
    if (version === 0) {
      return undefined
    }
    return new IpAddress(value, version === 4 ? "ipv4" : "ipv6")
  }

  /** 내부망, 루프백, 링크 로컬(인스턴스 메타데이터 포함), 멀티캐스트 대역인지 판단함 */
  isInternal(): boolean {
    return IpAddress.INTERNAL_RANGES.check(this.value, this.family)
  }
}
