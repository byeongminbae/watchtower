import { lookup } from 'node:dns/promises';
import { Injectable } from '@nestjs/common';

/** 호스트 이름에 등록된 IP 주소를 DNS 로 조회함 */
@Injectable()
export class DnsResolver {
  /** @return 조회에 실패하면 빈 배열 */
  async resolveAll(host: string): Promise<string[]> {
    try {
      const addresses = await lookup(host, { all: true, order: 'verbatim' });
      return addresses.map(({ address }) => address);
    } catch {
      return [];
    }
  }
}
