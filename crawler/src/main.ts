import "reflect-metadata"
import { NestFactory } from "@nestjs/core"
import { AppModule } from "./app.module.ts"
import { CrawlWorker } from "./crawl/crawl.worker.ts"

const app = await NestFactory.createApplicationContext(AppModule)
app.enableShutdownHooks()
app.get(CrawlWorker).start()
