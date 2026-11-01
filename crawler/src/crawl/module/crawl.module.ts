import { Module } from '@nestjs/common';
import { TimeProvider } from '../../common/time-provider.js';
import { OciModule } from '../../external/oci/oci.module.js';
import { PlaywrightModule } from '../../external/playwright/playwright.module.js';
import { CrawlService } from '../service/crawl.service.js';
import { CrawlRequestConsumer } from '../service/crawl-request-consumer.service.js';

@Module({
  imports: [OciModule, PlaywrightModule],
  providers: [TimeProvider, CrawlService, CrawlRequestConsumer]
})
export class CrawlModule {}
