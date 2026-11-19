import { lookup } from 'node:dns/promises';
import { Injectable } from '@nestjs/common';

@Injectable()
export class DnsResolver {
  async resolveAll(host: string): Promise<string[]> {
    const addresses = await lookup(host, { all: true, order: 'verbatim' }).catch(() => []);
    return addresses.map(({ address }) => address);
  }
}
