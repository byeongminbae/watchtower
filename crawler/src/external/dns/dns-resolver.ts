import { lookup } from 'node:dns/promises';
import { Injectable } from '@nestjs/common';

/** DNS 주소에 등록된 IP 주소를 조회하는 클래스 */
@Injectable()
export class DnsResolver {
  /** @return 조회에 실패하면 빈 배열 */
  async resolveAll(host: string): Promise<string[]> {
    try {
      const addresses = await lookup(host, { all: true, verbatim: true });
      return addresses.map(({ address }) => address);
    } catch {
      return [];
    }
  }
}
