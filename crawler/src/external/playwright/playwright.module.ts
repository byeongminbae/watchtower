import { Module } from "@nestjs/common"
import { PageFetcher } from "../../crawl/crawl.service.ts"
import { DnsResolver } from "../dns/dns-resolver.ts"
import { PlaywrightPageFetcher } from "./playwright-page-fetcher.ts"

@Module({
  providers: [DnsResolver, { provide: PageFetcher, useClass: PlaywrightPageFetcher }],
  exports: [PageFetcher]
})
export class PlaywrightModule {}
