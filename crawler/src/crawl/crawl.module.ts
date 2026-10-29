import { Module } from "@nestjs/common"
import { OciModule } from "../external/oci/oci.module.ts"
import { PlaywrightModule } from "../external/playwright/playwright.module.ts"
import { CrawlService, NOW } from "./crawl.service.ts"
import { CrawlWorker } from "./crawl.worker.ts"

@Module({
  imports: [OciModule, PlaywrightModule],
  providers: [
    { provide: NOW, useValue: () => new Date() },
    CrawlService,
    CrawlWorker
  ]
})
export class CrawlModule {}
