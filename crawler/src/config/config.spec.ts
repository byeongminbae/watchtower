import { ConfigLoader } from './config.js';

describe('ConfigLoader', () => {
  it('CRAWLER_PROFILE 이 없으면 인증 파일을 읽기 전에 실패함', () => {
    // given
    const env = {};

    // when
    const loading = () => ConfigLoader.load(env);

    // then
    expect(loading).toThrow('CRAWLER_PROFILE');
  });

  it('없는 프로필을 지정하면 인증 파일을 읽기 전에 실패함', () => {
    // given
    const env = { CRAWLER_PROFILE: 'prod' };

    // when
    const loading = () => ConfigLoader.load(env);

    // then
    expect(loading).toThrow('CRAWLER_PROFILE');
  });
});
