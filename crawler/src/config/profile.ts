import { type AuthenticationDetailsProvider, ConfigFileAuthenticationDetailsProvider } from 'oci-common';
import { SecretsClient } from 'oci-secrets';
import { z } from 'zod';
import { RUN_MODES } from './run-modes.js';

export type DeploymentEnvironment = 'local' | 'dev' | 'prod';

export type EnvironmentConfig = {
  readonly deploymentEnvironment: DeploymentEnvironment;
  readonly requestQueueId: string;
  readonly requestQueueEndpoint: string;
  readonly resultQueueId: string;
  readonly resultQueueEndpoint: string;
  readonly bucketName: string;
};

export type RunModeConfig = {
  readonly objectStorageNamespace: string;
  readonly concurrency: number;
  readonly environments: readonly EnvironmentConfig[];
  readonly secretVaultId: string;
};

export type RuntimeConfig = RunModeConfig & {
  readonly runModeName: string;
  readonly ociAuthentication: AuthenticationDetailsProvider;
  readonly slackWebhookUrl: string;
};

const SlackWebhookUrl = z.url({ protocol: /^https$/ });

/**
 * 크롤러 서버는 인스턴스 메타데이터 접근을 막아야 해서 Instance Principal 대신 ~/.oci/config 의 API 키로 인증함
 * Slack Webhook 주소 같은 비밀값은 저장소에 두지 않고 Vault 에서 읽음. 비밀값 하나에 값 하나를 그대로 저장함
 * 오류를 알릴 곳이 없는 채로 돌지 않도록, 비밀값을 읽지 못하면 기동을 멈춤
 */
export class ConfigLoader {
  private static readonly SLACK_WEBHOOK_URL_SECRET_NAME = 'crawler-slack-webhook-url';

  static async load(env: NodeJS.ProcessEnv): Promise<RuntimeConfig> {
    const runModeName = env.CRAWLER_RUN_MODE ?? '';
    const runModeConfig = RUN_MODES[runModeName];
    if (runModeConfig === undefined) {
      throw new Error(`CRAWLER_RUN_MODE 는 ${Object.keys(RUN_MODES).join(', ')} 중 하나여야 함`);
    }
    const ociAuthentication = new ConfigFileAuthenticationDetailsProvider();
    const secretsClient = new SecretsClient({ authenticationDetailsProvider: ociAuthentication });
    const slackWebhookUrl = SlackWebhookUrl.parse(
      await ConfigLoader.readSecret(
        secretsClient,
        runModeConfig.secretVaultId,
        ConfigLoader.SLACK_WEBHOOK_URL_SECRET_NAME
      )
    );
    return { ...runModeConfig, runModeName, ociAuthentication, slackWebhookUrl };
  }

  private static async readSecret(secretsClient: SecretsClient, vaultId: string, secretName: string): Promise<string> {
    const response = await secretsClient.getSecretBundleByName({ secretName, vaultId });
    const encodedContent = response.secretBundle.secretBundleContent?.content ?? '';
    return Buffer.from(encodedContent, 'base64').toString('utf8');
  }
}
