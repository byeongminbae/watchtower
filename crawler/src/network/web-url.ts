/** 공인 주소 여부는 CrawlHostValidator 로 따로 검사해야 함 */
export class WebUrl {
  private static readonly HTTP_PROTOCOLS: ReadonlySet<string> = new Set(['http:', 'https:']);
  private static readonly WEB_SOCKET_PROTOCOLS: ReadonlySet<string> = new Set(['ws:', 'wss:']);

  private constructor(
    private readonly protocol: string,
    readonly host: string
  ) {}

  static from(url: string): WebUrl | null {
    const parsedUrl = URL.parse(url);
    if (parsedUrl === null || parsedUrl.hostname === '') {
      return null;
    }
    return new WebUrl(parsedUrl.protocol, parsedUrl.hostname);
  }

  isHttp(): boolean {
    return WebUrl.HTTP_PROTOCOLS.has(this.protocol);
  }

  isWebSocket(): boolean {
    return WebUrl.WEB_SOCKET_PROTOCOLS.has(this.protocol);
  }
}
