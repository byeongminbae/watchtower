import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ConfigLoader } from './config/profile.js';
import { CrawlModule } from './crawl/crawl.module.js';
import { SlackModule } from './external/slack/slack.module.js';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true, load: [() => ConfigLoader.load(process.env)] }),
    SlackModule,
    CrawlModule
  ]
})
export class AppModule {}
