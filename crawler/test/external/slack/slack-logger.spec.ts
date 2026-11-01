import { ConsoleLogger, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { IncomingWebhook } from '@slack/webhook';
import { TimeProvider } from '../../../src/common/time-provider.js';
import type { RuntimeConfig } from '../../../src/config/config.js';
import { Policy } from '../../../src/config/policy.js';
import { SlackLogger } from '../../../src/external/slack/slack-logger.js';

const slackWebhookUrl = 'https://hooks.slack.com/services/test';

function createSlackLogger(timeProvider = new TimeProvider()): SlackLogger {
  return new SlackLogger(
    new ConfigService<RuntimeConfig, true>({ slackWebhookUrl, runModeName: 'server' }),
    timeProvider
  );
}

function sentTexts(): string[] {
  return vi
    .mocked(IncomingWebhook.prototype.send)
    .mock.calls.map(([message]) => (typeof message === 'string' ? message : (message.text ?? '')));
}

describe('SlackLogger', () => {
  beforeEach(() => {
    vi.spyOn(IncomingWebhook.prototype, 'send').mockResolvedValue({ text: 'ok' });
    vi.spyOn(process.stdout, 'write').mockReturnValue(true);
    vi.spyOn(process.stderr, 'write').mockReturnValue(true);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('error 로그를 남기면 실행 모드, 로그를 남긴 클래스와 오류 메시지, 스택 트레이스를 Slack 으로 보냄', async () => {
    // given
    const slackLogger = createSlackLogger();
    const error = new Error('큐 연결 끊김');

    // when
    slackLogger.error('[dev] 크롤링 요청 수신 실패', error, 'CrawlRequestConsumer');
    await slackLogger.onApplicationShutdown();

    // then
    const [text] = sentTexts();
    expect(text).toContain('[crawler:server] ERROR');
    expect(text).toContain('CrawlRequestConsumer');
    expect(text).toContain('[dev] 크롤링 요청 수신 실패');
    expect(text).toContain('Error: 큐 연결 끊김');
    expect(text).toContain('slack-logger.spec.ts');
  });

  it('각 클래스가 Nest Logger 로 남긴 오류도 클래스 이름과 함께 Slack 으로 보냄', async () => {
    // given
    const slackLogger = createSlackLogger();
    Logger.overrideLogger(slackLogger);
    const classLogger = new Logger('CrawlResultBatchService');

    // when
    classLogger.error('결과 3건 발행 실패. 요청을 다시 전달받아 처리함', new Error('큐 응답 없음'));
    await slackLogger.onApplicationShutdown();
    Logger.overrideLogger(new ConsoleLogger());

    // then
    const [text] = sentTexts();
    expect(text).toContain('CrawlResultBatchService');
    expect(text).toContain('결과 3건 발행 실패');
    expect(text).toContain('Error: 큐 응답 없음');
  });

  it('메시지만 남긴 로그는 상세 내용 없이 Slack 으로 보냄', async () => {
    // given
    const slackLogger = createSlackLogger();
    Logger.overrideLogger(slackLogger);
    const classLogger = new Logger('CrawlRequestConsumer');

    // when
    classLogger.warn('[dev] 크롤링 요청 수신 지연');
    await slackLogger.onApplicationShutdown();
    Logger.overrideLogger(new ConsoleLogger());

    // then
    const [text] = sentTexts();
    expect(text).toContain('[dev] 크롤링 요청 수신 지연');
    expect(text).not.toContain('undefined');
    expect(text).not.toContain('```');
  });

  it('객체 안에 담긴 오류도 스택 트레이스를 Slack 으로 보냄', async () => {
    // given
    const slackLogger = createSlackLogger();

    // when
    slackLogger.error('[prod] 크롤링 실패', { towerId: 7, error: new Error('브라우저 종료') }, 'CrawlRequestConsumer');
    await slackLogger.onApplicationShutdown();

    // then
    const [text] = sentTexts();
    expect(text).toContain('towerId: 7');
    expect(text).toContain('Error: 브라우저 종료');
    expect(text).toContain('slack-logger.spec.ts');
  });

  it('같은 로그가 억제 시간 안에 다시 남으면 Slack 으로 한 번만 보냄', async () => {
    // given
    const timeProvider = new TimeProvider();
    const startedAt = new Date('2026-11-02T00:00:00.000Z');
    vi.spyOn(timeProvider, 'now')
      .mockReturnValueOnce(startedAt)
      .mockReturnValue(new Date(startedAt.getTime() + Policy.SLACK_DUPLICATE_SUPPRESSION_MILLIS - 1));
    const slackLogger = createSlackLogger(timeProvider);

    // when
    slackLogger.error('[dev] 크롤링 요청 수신 실패', new Error('첫 번째'), 'CrawlRequestConsumer');
    slackLogger.error('[dev] 크롤링 요청 수신 실패', new Error('두 번째'), 'CrawlRequestConsumer');
    await slackLogger.onApplicationShutdown();

    // then
    expect(IncomingWebhook.prototype.send).toHaveBeenCalledTimes(1);
  });

  it('억제 시간이 지나면 같은 로그도 다시 Slack 으로 보냄', async () => {
    // given
    const timeProvider = new TimeProvider();
    const startedAt = new Date('2026-11-02T00:00:00.000Z');
    vi.spyOn(timeProvider, 'now')
      .mockReturnValueOnce(startedAt)
      .mockReturnValue(new Date(startedAt.getTime() + Policy.SLACK_DUPLICATE_SUPPRESSION_MILLIS));
    const slackLogger = createSlackLogger(timeProvider);

    // when
    slackLogger.error('[dev] 크롤링 요청 수신 실패', new Error('첫 번째'), 'CrawlRequestConsumer');
    slackLogger.error('[dev] 크롤링 요청 수신 실패', new Error('두 번째'), 'CrawlRequestConsumer');
    await slackLogger.onApplicationShutdown();

    // then
    expect(IncomingWebhook.prototype.send).toHaveBeenCalledTimes(2);
  });

  it('log 레벨 로그는 Slack 으로 보내지 않음', async () => {
    // given
    const slackLogger = createSlackLogger();

    // when
    slackLogger.log('Nest application successfully started', 'NestApplication');
    await slackLogger.onApplicationShutdown();

    // then
    expect(IncomingWebhook.prototype.send).not.toHaveBeenCalled();
  });

  it('Slack 전송이 실패해도 예외를 던지지 않음', async () => {
    // given
    vi.mocked(IncomingWebhook.prototype.send).mockRejectedValue(new Error('A request error occurred: fetch failed'));
    const slackLogger = createSlackLogger();

    // when
    const logging = async () => {
      slackLogger.error('[dev] 크롤링 요청 수신 실패', new Error('큐 연결 끊김'), 'CrawlRequestConsumer');
      await slackLogger.onApplicationShutdown();
    };

    // then
    await expect(logging()).resolves.toBeUndefined();
  });
});
