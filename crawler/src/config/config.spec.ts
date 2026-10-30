import { ConfigLoader } from './config.js';

describe('ConfigLoader', () => {
  it('Vault ID 가 없으면 Vault 를 읽기 전에 실패함', async () => {
    // given
    const env = { CRAWLER_VAULT_ID: '' };

    // when
    const loading = ConfigLoader.load(env);

    // then
    await expect(loading).rejects.toThrow('CRAWLER_VAULT_ID');
  });
});
