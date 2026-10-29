import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module.js';
import { CrawlWorker } from './crawl/crawl.worker.js';

async function bootstrap(): Promise<void> {
  const app = await NestFactory.createApplicationContext(AppModule);
  app.enableShutdownHooks();
  app.get(CrawlWorker).start();
}
await bootstrap();
