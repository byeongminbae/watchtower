/**
 * 크롤링이 정상적으로 끝나지 못한 이유. 결과 메시지로 백엔드에 전달됨
 *
 * 재시도해도 결과가 같은 실패만 여기에 정의함. 일시적인 인프라 오류는 결과로 보내지 않고 메시지를 재전달 받아 다시 처리함
 */
export class CrawlError {
  static readonly URL_NOT_ALLOWED = new CrawlError('CRW_001', '내부망 등 허용되지 않는 주소로 접근하려 함');
  static readonly NAVIGATION_TIMEOUT = new CrawlError('CRW_002', '제한 시간 안에 페이지를 불러오지 못함');
  static readonly HTML_TOO_LARGE = new CrawlError('CRW_003', 'HTML 크기가 수집 한도를 넘음');
  static readonly NAVIGATION_FAILED = new CrawlError('CRW_004', 'DNS 조회 실패, 연결 거부 등으로 페이지를 열지 못함');

  private constructor(
    readonly code: string,
    readonly description: string
  ) {}
}
