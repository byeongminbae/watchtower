import ipaddr from 'ipaddr.js';
import type { DnsResolver } from '../../external/dns/dns-resolver.js';

/**
 * 크롤링 한 건 동안 접근하려는 주소가 외부 주소인지 검사함. 같은 호스트는 한 번만 조회함
 *
 * 조회한 주소와 브라우저가 실제로 접속하는 주소가 같다는 보장이 없어서 DNS rebinding 을 막지는 못함
 * 크롤러 서버를 내부망과 분리하는 것이 1차 방어이고, 이 검사는 그 설정이 잘못됐을 때를 대비한 보조 방어임
 */
export class CrawlHostValidator {
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
    const addresses = await this.dnsResolver.resolveAll(CrawlHostValidator.stripIpv6Brackets(host));
    return addresses.length > 0 && addresses.every((address) => CrawlHostValidator.isExternalIpAddress(address));
  }

  private static isExternalIpAddress(address: string): boolean {
    return ipaddr.isValid(address) && ipaddr.process(address).range() === 'unicast';
  }

  private static stripIpv6Brackets(host: string): string {
    return host.startsWith('[') && host.endsWith(']') ? host.slice(1, -1) : host;
  }
}

/** http(s) 스킴과 호스트를 가진 절대 URL. 외부 주소 여부는 CrawlHostValidator 로 따로 검사해야 함 */
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
