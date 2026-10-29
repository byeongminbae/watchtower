import { Module } from "@nestjs/common"
import { PAGE_FETCHER } from "../../crawl/interfaces/page-fetcher.ts"
import { DnsResolver } from "../dns/dns-resolver.ts"
import { PlaywrightPageFetcher } from "./playwright-page-fetcher.ts"

@Module({
  providers: [DnsResolver, { provide: PAGE_FETCHER, useClass: PlaywrightPageFetcher }],
  exports: [PAGE_FETCHER]
})
export class PlaywrightModule {}
