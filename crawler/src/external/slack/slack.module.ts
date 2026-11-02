import { Module } from '@nestjs/common';
import { TimeProvider } from '../../common/time-provider.js';
import { SlackLogger } from './slack-logger.js';

@Module({
  providers: [SlackLogger, TimeProvider],
  exports: [SlackLogger]
})
export class SlackModule {}
