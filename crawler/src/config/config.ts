import { type AuthenticationDetailsProvider, ConfigFileAuthenticationDetailsProvider } from 'oci-common';
import { SecretsClient } from 'oci-secrets';
import { z } from 'zod';
import { RUN_MODES } from './run-modes.js';

export type DeploymentEnvironment = 'dev' | 'prod';

export type EnvironmentConfig = {
  deploymentEnvironment: DeploymentEnvironment;
  requestQueueId: string;
  requestQueueEndpoint: string;
  resultQueueId: string;
  resultQueueEndpoint: string;
  bucketName: string;
};

export type RunModeConfig = {
  objectStorageNamespace: string;
  concurrency: number;
  environments: EnvironmentConfig[];
  /** crawler-config 비밀값을 읽을 Vault */
  secretVaultId: string;
};

export type RuntimeConfig = RunModeConfig & {
  runModeName: string;
  ociAuthentication: AuthenticationDetailsProvider;
  slackWebhookUrl: string;
};

const CrawlerSecrets = z.object({ slackWebhookUrl: z.url({ protocol: /^https$/ }) });

/**
 * 환경변수 CRAWLER_RUN_MODE 로 고른 실행 모드(server, local)의 설정에 OCI 인증 정보를 붙여 크롤러 설정을 만듦
 * 크롤러 서버는 인스턴스 메타데이터 접근을 막아야 해서 Instance Principal 대신 ~/.oci/config 의 API 키로 인증함
 * Slack Webhook 주소 같은 비밀값은 저장소에 두지 않고 Vault 의 crawler-config 비밀값(JSON)에서 읽음
 * 오류를 알릴 곳이 없는 채로 돌지 않도록, 비밀값을 읽지 못하면 기동을 멈춤
 */
export class ConfigLoader {
  private static readonly SECRET_NAME = 'crawler-config';

  static async load(env: NodeJS.ProcessEnv): Promise<RuntimeConfig> {
    const runModeName = env.CRAWLER_RUN_MODE ?? '';
    const runModeConfig = RUN_MODES[runModeName];
    if (runModeConfig === undefined) {
      throw new Error(`CRAWLER_RUN_MODE 는 ${Object.keys(RUN_MODES).join(', ')} 중 하나여야 함`);
    }
    const ociAuthentication = new ConfigFileAuthenticationDetailsProvider();
    const crawlerSecrets = await ConfigLoader.loadCrawlerSecrets(ociAuthentication, runModeConfig.secretVaultId);
    return { ...runModeConfig, runModeName, ociAuthentication, slackWebhookUrl: crawlerSecrets.slackWebhookUrl };
  }

  private static async loadCrawlerSecrets(
    ociAuthentication: AuthenticationDetailsProvider,
    vaultId: string
  ): Promise<z.infer<typeof CrawlerSecrets>> {
    const secretsClient = new SecretsClient({ authenticationDetailsProvider: ociAuthentication });
    const response = await secretsClient.getSecretBundleByName({ secretName: ConfigLoader.SECRET_NAME, vaultId });
    const encodedContent = response.secretBundle.secretBundleContent?.content ?? '';
    return CrawlerSecrets.parse(JSON.parse(Buffer.from(encodedContent, 'base64').toString('utf8')));
  }
}
