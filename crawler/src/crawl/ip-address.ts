import { BlockList, isIP } from "node:net"

type IpFamily = "ipv4" | "ipv6"

export class IpAddress {
  private static readonly IPV4_MAPPED_PREFIX = "::ffff:"
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
    private readonly family: IpFamily
  ) {}

  /** @return IP 형식이 아니면 undefined */
  static from(value: string): IpAddress | undefined {
    const mappedIpv4 = IpAddress.unwrapMappedIpv4(value)
    if (mappedIpv4 !== undefined) {
      return new IpAddress(mappedIpv4, "ipv4")
    }

    const family = IpAddress.toFamily(value)
    return family === undefined ? undefined : new IpAddress(value, family)
  }

  isInternal(): boolean {
    return IpAddress.INTERNAL_RANGES.check(this.value, this.family)
  }

  private static unwrapMappedIpv4(value: string): string | undefined {
    if (!value.toLowerCase().startsWith(IpAddress.IPV4_MAPPED_PREFIX)) {
      return undefined
    }
    const ipv4 = value.slice(IpAddress.IPV4_MAPPED_PREFIX.length)
    return isIP(ipv4) === 4 ? ipv4 : undefined
  }

  private static toFamily(value: string): IpFamily | undefined {
    switch (isIP(value)) {
      case 4:
        return "ipv4"
      case 6:
        return "ipv6"
      default:
        return undefined
    }
  }
}
