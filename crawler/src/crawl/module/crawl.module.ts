import { Module } from '@nestjs/common';
import { TimeProvider } from '../../common/time-provider.js';
import { OciModule } from '../../external/oci/oci.module.js';
import { PlaywrightModule } from '../../external/playwright/playwright.module.js';
import { CrawlService } from '../service/crawl.service.js';
import { CrawlWorker } from '../crawl.worker.js';

@Module({
  imports: [OciModule, PlaywrightModule],
  providers: [TimeProvider, CrawlService, CrawlWorker]
})
export class CrawlModule {}
