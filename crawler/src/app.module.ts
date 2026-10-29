import { Global, Module } from "@nestjs/common"
import { CrawlerConfig } from "./config/crawler-config.ts"
import { CrawlModule } from "./crawl/crawl.module.ts"

@Global()
@Module({
  imports: [CrawlModule],
  providers: [{ provide: CrawlerConfig, useFactory: () => CrawlerConfig.from(process.env) }],
  exports: [CrawlerConfig]
})
export class AppModule {}
