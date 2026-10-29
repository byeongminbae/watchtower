import { BlockList, isIP } from 'node:net';
import type { DnsResolver } from '../external/dns/dns-resolver.js';

/**
 * 크롤링 한 건 동안 접근하려는 주소가 외부 주소인지 검사함. 같은 호스트는 한 번만 조회함
 *
 * 조회한 주소와 브라우저가 실제로 접속하는 주소가 같다는 보장이 없어서 DNS rebinding 을 막지는 못함
 * 크롤러 서버를 내부망과 분리하는 것이 1차 방어이고, 이 검사는 그 설정이 잘못됐을 때를 대비한 보조 방어임
 */
export class HostCheck {
  private readonly verdicts = new Map<string, Promise<boolean>>();

  constructor(private readonly dnsResolver: DnsResolver) {}

  async isAllowedUrl(url: string): Promise<boolean> {
    const httpUrl = HttpUrl.from(url);
    return httpUrl !== undefined && (await this.isAllowedHost(httpUrl.host));
  }

  private isAllowedHost(host: string): Promise<boolean> {
    let verdict = this.verdicts.get(host);
    if (verdict === undefined) {
      verdict = this.resolvesOnlyToExternalAddresses(host);
      this.verdicts.set(host, verdict);
    }
    return verdict;
  }

  private async resolvesOnlyToExternalAddresses(host: string): Promise<boolean> {
    const addresses = await this.dnsResolver.resolveAll(HostCheck.stripIpv6Brackets(host));
    return addresses.length > 0 && addresses.every((address) => HostCheck.isExternalIpAddress(address));
  }

  private static isExternalIpAddress(address: string): boolean {
    const ipAddress = IpAddress.from(address);
    return ipAddress !== undefined && !ipAddress.isInternal();
  }

  private static stripIpv6Brackets(host: string): string {
    return host.startsWith('[') && host.endsWith(']') ? host.slice(1, -1) : host;
  }
}

/** http(s) 스킴과 호스트를 가진 절대 URL. 외부 주소 여부는 HostCheck 로 따로 검사해야 함 */
export class HttpUrl {
  private constructor(readonly host: string) {}

  /** @return URL 형식이 아니거나 http(s) 가 아니면 undefined */
  static from(value: string): HttpUrl | undefined {
    if (!URL.canParse(value)) {
      return undefined;
    }
    const { protocol, hostname } = new URL(value);
    if (!HttpUrl.isHttpProtocol(protocol) || hostname === '') {
      return undefined;
    }
    return new HttpUrl(hostname);
  }

  private static isHttpProtocol(protocol: string): boolean {
    return protocol === 'http:' || protocol === 'https:';
  }
}

type IpFamily = 'ipv4' | 'ipv6';

class IpAddress {
  private static readonly IPV4_MAPPED_PREFIX = '::ffff:';
  private static readonly INTERNAL_RANGES = new BlockList();

  static {
    IpAddress.INTERNAL_RANGES.addSubnet('0.0.0.0', 8, 'ipv4');
    IpAddress.INTERNAL_RANGES.addSubnet('10.0.0.0', 8, 'ipv4');
    IpAddress.INTERNAL_RANGES.addSubnet('100.64.0.0', 10, 'ipv4');
    IpAddress.INTERNAL_RANGES.addSubnet('127.0.0.0', 8, 'ipv4');
    IpAddress.INTERNAL_RANGES.addSubnet('169.254.0.0', 16, 'ipv4');
    IpAddress.INTERNAL_RANGES.addSubnet('172.16.0.0', 12, 'ipv4');
    IpAddress.INTERNAL_RANGES.addSubnet('192.168.0.0', 16, 'ipv4');
    IpAddress.INTERNAL_RANGES.addSubnet('224.0.0.0', 4, 'ipv4');
    IpAddress.INTERNAL_RANGES.addAddress('::', 'ipv6');
    IpAddress.INTERNAL_RANGES.addAddress('::1', 'ipv6');
    IpAddress.INTERNAL_RANGES.addSubnet('fc00::', 7, 'ipv6');
    IpAddress.INTERNAL_RANGES.addSubnet('fe80::', 10, 'ipv6');
    IpAddress.INTERNAL_RANGES.addSubnet('fec0::', 10, 'ipv6');
    IpAddress.INTERNAL_RANGES.addSubnet('ff00::', 8, 'ipv6');
  }

  private constructor(
    private readonly value: string,
    private readonly family: IpFamily
  ) {}

  /** @return IP 형식이 아니면 undefined */
  static from(value: string): IpAddress | undefined {
    const mappedIpv4 = IpAddress.unwrapMappedIpv4(value);
    if (mappedIpv4 !== undefined) {
      return new IpAddress(mappedIpv4, 'ipv4');
    }

    const family = IpAddress.toFamily(value);
    return family === undefined ? undefined : new IpAddress(value, family);
  }

  isInternal(): boolean {
    return IpAddress.INTERNAL_RANGES.check(this.value, this.family);
  }

  private static unwrapMappedIpv4(value: string): string | undefined {
    if (!value.toLowerCase().startsWith(IpAddress.IPV4_MAPPED_PREFIX)) {
      return undefined;
    }
    const ipv4 = value.slice(IpAddress.IPV4_MAPPED_PREFIX.length);
    return isIP(ipv4) === 4 ? ipv4 : undefined;
  }

  private static toFamily(value: string): IpFamily | undefined {
    switch (isIP(value)) {
      case 4:
        return 'ipv4';
      case 6:
        return 'ipv6';
      default:
        return undefined;
    }
  }
}
