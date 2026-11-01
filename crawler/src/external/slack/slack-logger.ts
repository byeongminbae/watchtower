import { inspect } from 'node:util';
import { ConsoleLogger, Injectable, type LogLevel, type OnApplicationShutdown } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { IncomingWebhook } from '@slack/webhook';
import { TimeProvider } from '../../common/time-provider.js';
import type { RuntimeConfig } from '../../config/config.js';
import { Policy } from '../../config/policy.js';

/**
 * 콘솔에 남기는 로그 중 error, warn, fatal 을 Slack Incoming Webhook 으로도 보냄. 오류 메시지와 스택 트레이스를 함께 보냄
 *
 * Nest 가 시작할 때 남기는 log 레벨 로그는 배포마다 여러 건이 쌓이기만 해서 보내지 않음
 * 같은 로그는 Policy.SLACK_DUPLICATE_SUPPRESSION_MILLIS 동안 한 번만 보내고, Slack 전송에 실패해도 크롤링에는 영향을 주지 않음
 */
@Injectable()
export class SlackLogger extends ConsoleLogger implements OnApplicationShutdown {
  /** Slack 메시지가 너무 길어 읽기 어려워지지 않도록 상세 내용은 이 길이까지만 보냄 */
  private static readonly MAX_DETAIL_LENGTH = 3_000;
  private static readonly SEND_TIMEOUT_MILLIS = 5_000;

  private readonly incomingWebhook: IncomingWebhook;
  private readonly runModeName: string;
  private readonly lastSentAtByLog = new Map<string, number>();
  private readonly pendingSends = new Set<Promise<void>>();

  constructor(
    configService: ConfigService<RuntimeConfig, true>,
    private readonly timeProvider: TimeProvider
  ) {
    super();
    this.incomingWebhook = new IncomingWebhook(configService.get('slackWebhookUrl', { infer: true }), {
      timeout: SlackLogger.SEND_TIMEOUT_MILLIS
    });
    this.runModeName = configService.get('runModeName', { infer: true });
  }

  override error(message: unknown, ...optionalParams: unknown[]): void {
    super.error(message, ...optionalParams);
    this.sendToSlack('error', message, optionalParams);
  }

  override warn(message: unknown, ...optionalParams: unknown[]): void {
    super.warn(message, ...optionalParams);
    this.sendToSlack('warn', message, optionalParams);
  }

  override fatal(message: unknown, ...optionalParams: unknown[]): void {
    super.fatal(message, ...optionalParams);
    this.sendToSlack('fatal', message, optionalParams);
  }

  /** 종료 직전에 남긴 로그도 Slack 에 도착하도록 보내는 중인 요청이 끝날 때까지 기다림 */
  async onApplicationShutdown(): Promise<void> {
    await Promise.allSettled(this.pendingSends);
  }

  private sendToSlack(logLevel: LogLevel, message: unknown, optionalParams: unknown[]): void {
    const [context, details] = SlackLogger.splitContext(optionalParams);
    const messageText = typeof message === 'string' ? message : inspect(message);
    if (this.isDuplicate(`${logLevel}:${context}:${messageText}`)) {
      return;
    }

    const title = `*[crawler:${this.runModeName}] ${logLevel.toUpperCase()}* ${context}\n${messageText}`;
    const text = SlackLogger.toSlackText(title, details);
    const pendingSend = this.post(text).finally(() => this.pendingSends.delete(pendingSend));
    this.pendingSends.add(pendingSend);
  }

  private isDuplicate(logKey: string): boolean {
    const now = this.timeProvider.now().getTime();
    const lastSentAt = this.lastSentAtByLog.get(logKey);
    if (lastSentAt !== undefined && now - lastSentAt < Policy.SLACK_DUPLICATE_SUPPRESSION_MILLIS) {
      return true;
    }
    this.lastSentAtByLog.set(logKey, now);
    return false;
  }

  /** Slack SDK 의 오류 메시지에는 응답 상태와 본문만 담기고 웹훅 주소는 담기지 않음 */
  private async post(text: string): Promise<void> {
    try {
      await this.incomingWebhook.send({ text });
    } catch (error) {
      super.warn(`Slack 로그 전송 실패. ${error instanceof Error ? error.message : String(error)}`, SlackLogger.name);
    }
  }

  /**
   * Nest 의 Logger 는 마지막 인자로 로그를 남긴 클래스 이름(context)을 붙여 넘김
   * 메시지만 남기면 [undefined, context] 로 넘기므로 undefined 는 상세 내용에서 뺌
   */
  private static splitContext(optionalParams: unknown[]): [string, unknown[]] {
    const lastParam = optionalParams.at(-1);
    if (typeof lastParam === 'string') {
      return [lastParam, optionalParams.slice(0, -1).filter((param) => param !== undefined)];
    }
    return ['', optionalParams.filter((param) => param !== undefined)];
  }

  private static toSlackText(title: string, details: unknown[]): string {
    if (details.length === 0) {
      return title;
    }

    const detailText = details
      .map((detail) => (typeof detail === 'string' ? detail : inspect(detail, { depth: 5 })))
      .join('\n')
      .slice(0, SlackLogger.MAX_DETAIL_LENGTH);
    return `${title}\n\`\`\`${detailText}\`\`\``;
  }
}
