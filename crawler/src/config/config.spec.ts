import { ConfigLoader } from './config.js';

const apiKeyEnv = {
  OCI_AUTHENTICATION: 'API_KEY',
  OCI_REGION: 'ap-osaka-1',
  OCI_TENANCY_ID: 'tenancy',
  OCI_USER_ID: 'user',
  OCI_FINGERPRINT: 'fingerprint',
  OCI_PRIVATE_KEY: 'private-key',
  CRAWLER_VAULT_ID: 'vault'
};

describe('ConfigLoader', () => {
  it('인증 방식을 지정하지 않으면 Vault 를 읽기 전에 실패함', async () => {
    // given
    const env = { ...apiKeyEnv, OCI_AUTHENTICATION: undefined };

    // when
    const loading = ConfigLoader.load(env);

    // then
    await expect(loading).rejects.toThrow('OCI_AUTHENTICATION');
  });

  it('API 키 인증인데 키 정보가 빠지면 Vault 를 읽기 전에 빠진 이름을 담아 실패함', async () => {
    // given
    const env = { ...apiKeyEnv, OCI_PRIVATE_KEY: '' };

    // when
    const loading = ConfigLoader.load(env);

    // then
    await expect(loading).rejects.toThrow('OCI_PRIVATE_KEY');
  });

  it('Vault ID 가 없으면 Vault 를 읽기 전에 실패함', async () => {
    // given
    const env = { ...apiKeyEnv, CRAWLER_VAULT_ID: undefined };

    // when
    const loading = ConfigLoader.load(env);

    // then
    await expect(loading).rejects.toThrow('CRAWLER_VAULT_ID');
  });
});
