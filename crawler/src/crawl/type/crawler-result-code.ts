/**
 * 실패 코드는 재시도해도 결과가 같은 실패만 정의함. 일시적인 인프라 오류는 결과로 보내지 않고 메시지를 재전달 받아 다시 처리함
 */
export type CrawlerResultCode =
  'FETCHED' | 'URL_NOT_ALLOWED' | 'NAVIGATION_TIMEOUT' | 'TEXT_TOO_LARGE' | 'NAVIGATION_FAILED' | 'INVALID_SELECTOR';

export type CrawlFailureCode = Exclude<CrawlerResultCode, 'FETCHED'>;
