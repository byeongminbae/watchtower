import { type AuthenticationDetailsProvider, ConfigFileAuthenticationDetailsProvider } from 'oci-common';
import { PROFILES } from './profiles.js';

export type EnvironmentConfig = {
  name: string;
  requestQueueId: string;
  requestQueueEndpoint: string;
  resultQueueId: string;
  resultQueueEndpoint: string;
  bucketName: string;
};

export type Settings = {
  objectStorageNamespace: string;
  concurrency: number;
  environments: EnvironmentConfig[];
};

export type Config = Settings & { ociAuthentication: AuthenticationDetailsProvider };

/**
 * 환경변수로 CRAWLER_PROFILE 를 설정하면 해당하는 프로필로 운영
 * 크롤러 서버는 인스턴스 메타데이터 접근을 막아야 해서 Instance Principal 대신 ~/.oci/config 의 API 키로 인증함
 */
export class ConfigLoader {
  static load(env: NodeJS.ProcessEnv): Config {
    const profile = env.CRAWLER_PROFILE ?? '';
    const settings = PROFILES[profile];
    if (settings === undefined) {
      throw new Error(`CRAWLER_PROFILE 은 ${Object.keys(PROFILES).join(', ')} 중 하나여야 함`);
    }
    return { ...settings, ociAuthentication: new ConfigFileAuthenticationDetailsProvider() };
  }
}
