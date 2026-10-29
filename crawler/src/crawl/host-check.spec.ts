import { HostCheck } from './host-check.js';
import { DnsResolver } from '../external/dns/dns-resolver.js';

function createDnsResolver(records: Record<string, string[]>): DnsResolver {
  const dnsResolver = new DnsResolver();
  vi.spyOn(dnsResolver, 'resolveAll').mockImplementation(async (host) => records[host] ?? []);
  return dnsResolver;
}

describe('HostCheck', () => {
  it('외부 IPv4와 IPv6 주소로만 조회되는 호스트는 접근을 허용함', async () => {
    for (const address of ['8.8.8.8', '2001:4860:4860::8888']) {
      // given
      const hostCheck = new HostCheck(createDnsResolver({ 'target.example': [address] }));

      // when
      const result = await hostCheck.isAllowedUrl('https://target.example/');

      // then
      expect(result, address).toBe(true);
    }
  });

  it('내부망, 메타데이터, 멀티캐스트 주소로 조회되는 호스트는 접근을 허용하지 않음', async () => {
    for (const address of [
      '0.0.0.0',
      '127.0.0.1',
      '10.0.0.163',
      '100.64.0.1',
      '172.16.0.1',
      '192.168.0.1',
      '169.254.169.254',
      '224.0.0.1',
      '::',
      '::1',
      'fc00::1',
      'fd12:3456::1',
      'fe80::1',
      'fec0::1',
      'ff02::1'
    ]) {
      // given
      const hostCheck = new HostCheck(createDnsResolver({ 'target.example': [address] }));

      // when
      const result = await hostCheck.isAllowedUrl('https://target.example/');

      // then
      expect(result, address).toBe(false);
    }
  });

  it('IPv6 형식으로 감싼 내부 IPv4 주소로 조회되는 호스트도 접근을 허용하지 않음', async () => {
    // given
    const hostCheck = new HostCheck(createDnsResolver({ 'target.example': ['::ffff:169.254.169.254'] }));

    // when
    const result = await hostCheck.isAllowedUrl('https://target.example/');

    // then
    expect(result).toBe(false);
  });

  it('IP 형식이 아닌 조회 결과는 판단할 수 없으므로 접근을 허용하지 않음', async () => {
    // given
    const hostCheck = new HostCheck(createDnsResolver({ 'target.example': ['not-an-ip'] }));

    // when
    const result = await hostCheck.isAllowedUrl('https://target.example/');

    // then
    expect(result).toBe(false);
  });

  it('조회된 주소가 모두 외부 주소일 때만 접근을 허용함', async () => {
    // given
    const hostCheck = new HostCheck(
      createDnsResolver({
        'example.com': ['93.184.216.34', '2606:2800:220:1::1'],
        'mixed.example': ['8.8.8.8', '10.0.0.1']
      })
    );

    // when
    const results = await Promise.all([
      hostCheck.isAllowedUrl('https://example.com/'),
      hostCheck.isAllowedUrl('https://mixed.example/')
    ]);

    // then
    expect(results).toEqual([true, false]);
  });

  it('조회 결과가 없으면 접근을 허용하지 않음', async () => {
    // given
    const hostCheck = new HostCheck(createDnsResolver({}));

    // when
    const result = await hostCheck.isAllowedUrl('https://unknown.example/');

    // then
    expect(result).toBe(false);
  });

  it('한 번의 크롤링 안에서 같은 호스트는 한 번만 조회함', async () => {
    // given
    const dnsResolver = createDnsResolver({ 'example.com': ['8.8.8.8'] });
    const hostCheck = new HostCheck(dnsResolver);

    // when
    await Promise.all([hostCheck.isAllowedUrl('https://example.com/'), hostCheck.isAllowedUrl('https://example.com/')]);

    // then
    expect(dnsResolver.resolveAll).toHaveBeenCalledTimes(1);
  });

  it('대괄호로 감싼 IPv6 호스트는 대괄호를 벗겨서 조회함', async () => {
    // given
    const dnsResolver = createDnsResolver({ '::1': ['::1'] });
    const hostCheck = new HostCheck(dnsResolver);

    // when
    const result = await hostCheck.isAllowedUrl('http://[::1]/');

    // then
    expect(dnsResolver.resolveAll).toHaveBeenCalledWith('::1');
    expect(result).toBe(false);
  });

  it('http(s) 가 아닌 주소는 호스트를 조회하지 않고 거부함', async () => {
    // given
    const dnsResolver = createDnsResolver({ 'example.com': ['8.8.8.8'] });
    const hostCheck = new HostCheck(dnsResolver);

    // when
    const result = await hostCheck.isAllowedUrl('ftp://example.com/file');

    // then
    expect(result).toBe(false);
    expect(dnsResolver.resolveAll).not.toHaveBeenCalled();
  });
});
