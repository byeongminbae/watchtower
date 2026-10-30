import { Module } from '@nestjs/common';
import { PageFetcher } from '../../crawl/interfaces/page-fetcher.interface.js';
import { DnsResolver } from '../dns/dns-resolver.js';
import { PlaywrightPageFetcher } from './playwright-page-fetcher.js';

@Module({
  providers: [DnsResolver, { provide: PageFetcher, useClass: PlaywrightPageFetcher }],
  exports: [PageFetcher]
})
export class PlaywrightModule {}
