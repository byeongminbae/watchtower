import { InternalNetworkIsolationCheck } from '../../../src/external/network/internal-network-isolation-check.js';

describe('InternalNetworkIsolationCheck', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('인스턴스 메타데이터에서 응답이 오면 호스트 방화벽이 빠진 것이라 기동을 멈춤', async () => {
    // given
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{}', { status: 401 }));

    // when
    const starting = () => new InternalNetworkIsolationCheck().onModuleInit();

    // then
    await expect(starting).rejects.toThrow('infra/crawler-firewall.sh');
  });

  it('인스턴스 메타데이터에 연결하지 못하면 차단된 것이라 그대로 기동함', async () => {
    // given
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('fetch failed'));

    // when
    const starting = () => new InternalNetworkIsolationCheck().onModuleInit();

    // then
    await expect(starting()).resolves.toBeUndefined();
  });
});
