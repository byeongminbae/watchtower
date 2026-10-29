import { Module } from "@nestjs/common"
import { createHostGuard } from "../../crawl/address-policy.ts"
import { PAGE_FETCHER } from "../../crawl/crawl.service.ts"
import { lookupAll } from "../dns/lookup-all.ts"
import { HOST_GUARD_FACTORY, PlaywrightPageFetcher } from "./playwright-page-fetcher.ts"

@Module({
  providers: [
    { provide: HOST_GUARD_FACTORY, useValue: () => createHostGuard(lookupAll) },
    PlaywrightPageFetcher,
    { provide: PAGE_FETCHER, useExisting: PlaywrightPageFetcher }
  ],
  exports: [PAGE_FETCHER]
})
export class PlaywrightModule {}
