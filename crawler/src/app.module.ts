import { Global, Module } from "@nestjs/common"
import { CRAWLER_CONFIG, readConfig } from "./config/crawler-config.ts"
import { CrawlModule } from "./crawl/crawl.module.ts"

@Global()
@Module({
  imports: [CrawlModule],
  providers: [{ provide: CRAWLER_CONFIG, useFactory: () => readConfig(process.env) }],
  exports: [CRAWLER_CONFIG]
})
export class AppModule {}
