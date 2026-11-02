import { ConfigLoader } from '../../src/config/config.js';

const { getSecretBundleByName } = vi.hoisted(() => ({ getSecretBundleByName: vi.fn() }));

vi.mock('oci-common', () => ({ ConfigFileAuthenticationDetailsProvider: class {} }));
vi.mock('oci-secrets', () => ({
  SecretsClient: class {
    getSecretBundleByName = getSecretBundleByName;
  }
}));

function mockSecretContent(content: string): void {
  getSecretBundleByName.mockResolvedValue({
    secretBundle: { secretBundleContent: { content: Buffer.from(content).toString('base64') } }
  });
}

describe('ConfigLoader', () => {
  afterEach(() => {
    getSecretBundleByName.mockReset();
  });

  it('CRAWLER_RUN_MODE 가 없으면 인증 파일을 읽기 전에 실패함', async () => {
    // given
    const env = {};

    // when
    const loading = () => ConfigLoader.load(env);

    // then
    await expect(loading).rejects.toThrow('CRAWLER_RUN_MODE');
  });

  it('없는 실행 모드를 지정하면 인증 파일을 읽기 전에 실패함', async () => {
    // given
    const env = { CRAWLER_RUN_MODE: 'prod' };

    // when
    const loading = () => ConfigLoader.load(env);

    // then
    await expect(loading).rejects.toThrow('CRAWLER_RUN_MODE');
  });

  it('Vault 의 crawler-slack-webhook-url 비밀값을 그대로 Slack Webhook 주소로 씀', async () => {
    // given
    const slackWebhookUrl = 'https://hooks.slack.com/services/test';
    mockSecretContent(slackWebhookUrl);

    // when
    const config = await ConfigLoader.load({ CRAWLER_RUN_MODE: 'local' });

    // then
    expect(config.slackWebhookUrl).toBe(slackWebhookUrl);
    expect(getSecretBundleByName).toHaveBeenCalledWith(
      expect.objectContaining({ secretName: 'crawler-slack-webhook-url' })
    );
  });

  it('Slack Webhook 비밀값이 https 주소가 아니면 기동을 멈춤', async () => {
    // given
    mockSecretContent('{"slackWebhookUrl":"https://hooks.slack.com/services/test"}');

    // when
    const loading = () => ConfigLoader.load({ CRAWLER_RUN_MODE: 'local' });

    // then
    await expect(loading).rejects.toThrow('Invalid URL');
  });
});
