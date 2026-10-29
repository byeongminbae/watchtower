/** http(s) 스킴과 호스트를 가진 절대 URL. 외부 주소 여부는 HostCheck 로 따로 검사해야 함 */
export class HttpUrl {
  private constructor(readonly host: string) {}

  /** @return URL 형식이 아니거나 http(s) 가 아니면 undefined */
  static from(value: string): HttpUrl | undefined {
    if (!URL.canParse(value)) {
      return undefined;
    }
    const { protocol, hostname } = new URL(value);
    if (!HttpUrl.isHttpProtocol(protocol) || hostname === '') {
      return undefined;
    }
    return new HttpUrl(hostname);
  }

  private static isHttpProtocol(protocol: string): boolean {
    return protocol === 'http:' || protocol === 'https:';
  }
}
