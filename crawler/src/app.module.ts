import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ConfigLoader } from './config/config.js';
import { CrawlModule } from './crawl/module/crawl.module.js';

@Module({
  imports: [ConfigModule.forRoot({ isGlobal: true, load: [() => ConfigLoader.load(process.env)] }), CrawlModule]
})
export class AppModule {}
