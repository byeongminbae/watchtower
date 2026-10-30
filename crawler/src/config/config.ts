import {
  type AuthenticationDetailsProvider,
  ConfigFileAuthenticationDetailsProvider,
  Region,
  SimpleAuthenticationDetailsProvider
} from 'oci-common';
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
 * 설정은 Vault 의 crawler-config 비밀값(JSON)에서 읽고, 환경변수로는 OCI 인증 정보와 Vault ID 만 받음
 * 크롤러 서버는 인스턴스 메타데이터 접근을 막아야 해서 Instance Principal 대신 API 키로 인증함
 */
export class ConfigLoader {
  private static readonly SECRET_NAME = 'crawler-config';

  static async load(env: NodeJS.ProcessEnv): Promise<Config> {
    const ociAuthentication = ConfigLoader.toOciAuthentication(env);
    const vaultId = ConfigLoader.required(env, 'CRAWLER_VAULT_ID');
    const client = new SecretsClient({ authenticationDetailsProvider: ociAuthentication });
    const response = await client.getSecretBundleByName({ secretName: ConfigLoader.SECRET_NAME, vaultId });
    const content = Buffer.from(response.secretBundle.secretBundleContent?.content ?? '', 'base64').toString('utf8');
    const settings = JSON.parse(content) as Settings;
    if (!Number.isSafeInteger(settings.concurrency) || settings.concurrency < 1) {
      throw new Error('concurrency 는 1 이상의 정수여야 함');
    }
    return { ...settings, ociAuthentication };
  }

  private static toOciAuthentication(env: NodeJS.ProcessEnv): AuthenticationDetailsProvider {
    switch (env.OCI_AUTHENTICATION) {
      case 'CONFIG_FILE':
        return new ConfigFileAuthenticationDetailsProvider();
      case 'API_KEY':
        return new SimpleAuthenticationDetailsProvider(
          ConfigLoader.required(env, 'OCI_TENANCY_ID'),
          ConfigLoader.required(env, 'OCI_USER_ID'),
          ConfigLoader.required(env, 'OCI_FINGERPRINT'),
          ConfigLoader.required(env, 'OCI_PRIVATE_KEY'),
          null,
          Region.fromRegionId(ConfigLoader.required(env, 'OCI_REGION'))
        );
      default:
        throw new Error('OCI_AUTHENTICATION 은 API_KEY 또는 CONFIG_FILE 이어야 함');
    }
  }

  private static required(env: NodeJS.ProcessEnv, name: string): string {
    const value = env[name];
    if (value === undefined || value.trim() === '') {
      throw new Error(`필수 환경변수 ${name} 이 없음`);
    }
    return value;
  }
}
