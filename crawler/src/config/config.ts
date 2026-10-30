import { type AuthenticationDetailsProvider, ConfigFileAuthenticationDetailsProvider } from 'oci-common';
import { SecretsClient } from 'oci-secrets';

export type CrawlEnvironmentConfig = {
  name: string;
  requestQueueId: string;
  requestQueueEndpoint: string;
  resultQueueId: string;
  resultQueueEndpoint: string;
  bucketName: string;
};

type Settings = {
  objectStorageNamespace: string;
  concurrency: number;
  environments: CrawlEnvironmentConfig[];
};

export type Config = Settings & { ociAuthentication: AuthenticationDetailsProvider };

/**
 * 설정은 Vault 의 crawler-config 비밀값(JSON)에서 읽고, 환경변수로는 Vault ID 만 받음
 * 크롤러 서버는 인스턴스 메타데이터 접근을 막아야 해서 Instance Principal 대신 ~/.oci/config 의 API 키로 인증함
 */
export class ConfigLoader {
  private static readonly SECRET_NAME = 'crawler-config';

  static async load(env: NodeJS.ProcessEnv): Promise<Config> {
    const vaultId = env.CRAWLER_VAULT_ID;
    if (vaultId === undefined || vaultId.trim() === '') {
      throw new Error('필수 환경변수 CRAWLER_VAULT_ID 가 없음');
    }
    const ociAuthentication = new ConfigFileAuthenticationDetailsProvider();
    const client = new SecretsClient({ authenticationDetailsProvider: ociAuthentication });
    const response = await client.getSecretBundleByName({ secretName: ConfigLoader.SECRET_NAME, vaultId });
    const content = Buffer.from(response.secretBundle.secretBundleContent?.content ?? '', 'base64').toString('utf8');
    const settings = JSON.parse(content) as Settings;
    if (!Number.isSafeInteger(settings.concurrency) || settings.concurrency < 1) {
      throw new Error('concurrency 는 1 이상의 정수여야 함');
    }
    return { ...settings, ociAuthentication };
  }
}
