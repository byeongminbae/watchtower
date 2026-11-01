import { type AuthenticationDetailsProvider, ConfigFileAuthenticationDetailsProvider } from 'oci-common';
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
};

export type RuntimeConfig = RunModeConfig & { ociAuthentication: AuthenticationDetailsProvider };

/**
 * 환경변수로 CRAWLER_RUN_MODE 를 설정하면 해당하는 실행 모드로 운영
 * 크롤러 서버는 인스턴스 메타데이터 접근을 막아야 해서 Instance Principal 대신 ~/.oci/config 의 API 키로 인증함
 */
export class ConfigLoader {
  static load(env: NodeJS.ProcessEnv): RuntimeConfig {
    const runModeName = env.CRAWLER_RUN_MODE ?? '';
    const runModeConfig = RUN_MODES[runModeName];
    if (runModeConfig === undefined) {
      throw new Error(`CRAWLER_RUN_MODE 는 ${Object.keys(RUN_MODES).join(', ')} 중 하나여야 함`);
    }
    return { ...runModeConfig, ociAuthentication: new ConfigFileAuthenticationDetailsProvider() };
  }
}
