import { Module } from '@nestjs/common';
import { PAGE_FETCHER } from '../../crawl/interfaces/page-fetcher.interface.js';
import { DnsResolver } from '../dns/dns-resolver.js';
import { PlaywrightPageFetcher } from './playwright-page-fetcher.js';

@Module({
  providers: [DnsResolver, { provide: PAGE_FETCHER, useClass: PlaywrightPageFetcher }],
  exports: [PAGE_FETCHER]
})
export class PlaywrightModule {}
