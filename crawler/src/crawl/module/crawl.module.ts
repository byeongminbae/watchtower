import { Module } from '@nestjs/common';
import { TimeProvider } from '../../common/time-provider.js';
import { OciModule } from '../../external/oci/oci.module.js';
import { PlaywrightModule } from '../../external/playwright/playwright.module.js';
import { CrawlService } from '../service/crawl.service.js';
import { CrawlWorkerService } from '../service/crawl-worker.service.js';

@Module({
  imports: [OciModule, PlaywrightModule],
  providers: [TimeProvider, CrawlService, CrawlWorkerService]
})
export class CrawlModule {}
