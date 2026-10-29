/**
 * 크롤링이 정상적으로 끝나지 못한 이유. 결과 메시지로 백엔드에 전달됨
 *
 * 재시도해도 결과가 같은 실패만 여기에 정의함. 일시적인 인프라 오류는 결과로 보내지 않고 메시지를 재전달 받아 다시 처리함
 */
export const CrawlError = {
  URL_NOT_ALLOWED: { code: "CRW_001", description: "내부망 등 허용되지 않는 주소로 접근하려 함" },
  NAVIGATION_TIMEOUT: { code: "CRW_002", description: "제한 시간 안에 페이지를 불러오지 못함" },
  HTML_TOO_LARGE: { code: "CRW_003", description: "HTML 크기가 수집 한도를 넘음" },
  NAVIGATION_FAILED: { code: "CRW_004", description: "DNS 조회 실패, 연결 거부 등으로 페이지를 열지 못함" }
} as const

export type CrawlErrorType = (typeof CrawlError)[keyof typeof CrawlError]
