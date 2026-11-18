import { Module } from '@nestjs/common';
import { TimeProvider } from '../common/time-provider.js';
import { OciModule } from '../external/oci/oci.module.js';
import { PlaywrightModule } from '../external/playwright/playwright.module.js';
import { InternalNetworkIsolationCheck } from '../network/internal-network-isolation-check.js';
import { CrawlRequestConsumer } from './crawl-request-consumer.js';
import { CrawlService } from './crawl.service.js';

@Module({
  imports: [OciModule, PlaywrightModule],
  providers: [InternalNetworkIsolationCheck, TimeProvider, CrawlService, CrawlRequestConsumer]
})
export class CrawlModule {}
