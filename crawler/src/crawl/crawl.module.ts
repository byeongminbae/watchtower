import { Module } from "@nestjs/common"
import { TimeProvider } from "../common/time-provider.ts"
import { OciModule } from "../external/oci/oci.module.ts"
import { PlaywrightModule } from "../external/playwright/playwright.module.ts"
import { CrawlService } from "./crawl.service.ts"
import { CrawlWorker } from "./crawl.worker.ts"

@Module({
  imports: [OciModule, PlaywrightModule],
  providers: [TimeProvider, CrawlService, CrawlWorker]
})
export class CrawlModule {}
