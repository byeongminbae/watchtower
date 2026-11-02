import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module.js';
import { SlackLogger } from './external/slack/slack-logger.js';

async function bootstrap(): Promise<void> {
  const app = await NestFactory.createApplicationContext(AppModule, { bufferLogs: true });
  app.useLogger(app.get(SlackLogger));
  app.enableShutdownHooks();
}
await bootstrap();
