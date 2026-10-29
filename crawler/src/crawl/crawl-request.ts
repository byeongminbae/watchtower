/**
 * 백엔드 스케줄러가 요청 큐에 넣는 크롤링 요청
 *
 * @property towerId 크롤링할 타워
 * @property url 백엔드에서 검증했더라도 크롤러는 이 값을 신뢰하지 않고 다시 검사함
 * @property scheduledAt 백엔드가 정한 실행 예정 시각(ISO 8601, UTC). towerId 와 함께 결과의 중복 반영을 막는 키로 쓰임
 */
export type CrawlRequest = {
  towerId: number
  url: string
  scheduledAt: string
}

/** @return 형식이 맞지 않거나 http(s) 가 아닌 URL 이면 undefined */
export function parseCrawlRequest(content: string): CrawlRequest | undefined {
  let message: unknown
  try {
    message = JSON.parse(content)
  } catch {
    return undefined
  }

  if (typeof message !== "object" || message === null) {
    return undefined
  }

  const { towerId, url, scheduledAt } = message as Record<string, unknown>
  if (!Number.isSafeInteger(towerId) || (towerId as number) <= 0) {
    return undefined
  }
  if (typeof url !== "string" || !isHttpUrl(url)) {
    return undefined
  }
  if (typeof scheduledAt !== "string" || Number.isNaN(Date.parse(scheduledAt))) {
    return undefined
  }

  return { towerId: towerId as number, url, scheduledAt }
}

export function isHttpUrl(value: string): boolean {
  if (!URL.canParse(value)) {
    return false
  }
  const { protocol, hostname } = new URL(value)
  return (protocol === "http:" || protocol === "https:") && hostname !== ""
}
