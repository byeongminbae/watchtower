import { ConfigLoader } from '../../src/config/config.js';

describe('ConfigLoader', () => {
  it('CRAWLER_RUN_MODE 가 없으면 인증 파일을 읽기 전에 실패함', () => {
    // given
    const env = {};

    // when
    const loading = () => ConfigLoader.load(env);

    // then
    expect(loading).toThrow('CRAWLER_RUN_MODE');
  });

  it('없는 실행 모드를 지정하면 인증 파일을 읽기 전에 실패함', () => {
    // given
    const env = { CRAWLER_RUN_MODE: 'prod' };

    // when
    const loading = () => ConfigLoader.load(env);

    // then
    expect(loading).toThrow('CRAWLER_RUN_MODE');
  });
});
