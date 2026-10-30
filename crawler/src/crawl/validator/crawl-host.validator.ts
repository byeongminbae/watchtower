import ipaddr from 'ipaddr.js';
import type { DnsResolver } from '../../external/dns/dns-resolver.js';
import { HttpUrl } from '../vo/http-url.vo.js';

/**
 * 크롤링 한 건 동안 접근하려는 주소가 외부 주소인지 검사함. 같은 호스트는 한 번만 조회함
 *
 * 조회한 주소와 브라우저가 실제로 접속하는 주소가 같다는 보장이 없어서 DNS rebinding 을 막지는 못함
 * 크롤러 서버를 내부망과 분리하는 것이 1차 방어이고, 이 검사는 그 설정이 잘못됐을 때를 대비한 보조 방어임
 */
export class CrawlHostValidator {
  private readonly hostCheckCache = new Map<string, Promise<boolean>>();

  constructor(private readonly dnsResolver: DnsResolver) {}

  async isAllowedUrl(url: string): Promise<boolean> {
    const parsedUrl = HttpUrl.from(url);
    return parsedUrl !== undefined && (await this.isAllowedHost(parsedUrl.host));
  }

  private isAllowedHost(host: string): Promise<boolean> {
    let hostCheck = this.hostCheckCache.get(host);
    if (hostCheck === undefined) {
      hostCheck = this.resolvesOnlyToPublicIpAddresses(host);
      this.hostCheckCache.set(host, hostCheck);
    }
    return hostCheck;
  }

  private async resolvesOnlyToPublicIpAddresses(host: string): Promise<boolean> {
    const resolvedIpAddresses = await this.dnsResolver.resolveAll(CrawlHostValidator.stripIpv6Brackets(host));
    return (
      resolvedIpAddresses.length > 0 &&
      resolvedIpAddresses.every((ipAddress) => CrawlHostValidator.isPublicIpAddress(ipAddress))
    );
  }

  private static isPublicIpAddress(ipAddress: string): boolean {
    return ipaddr.isValid(ipAddress) && ipaddr.process(ipAddress).range() === 'unicast';
  }

  private static stripIpv6Brackets(host: string): string {
    return host.startsWith('[') && host.endsWith(']') ? host.slice(1, -1) : host;
  }
}
