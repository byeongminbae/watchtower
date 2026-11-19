import type { CrawlerResultCode, WatchConditionInput, WatchDetection } from "@/types/tower";

/** 백엔드 CssSelector.MAX_LENGTH 와 같은 값 */
export const MAX_DETECTION_AREA_SELECTOR_LENGTH = 500;

const UNKNOWN_CRAWL_FAILURE = "사이트가 응답하지 않거나 접근할 수 없는 페이지예요. 사이트를 직접 열어 주소를 확인한 뒤 다시 확인해 주세요.";
const CRAWL_FAILURE_DESCRIPTIONS: Record<CrawlerResultCode, string> = {
  FETCHED: "사이트가 오류 응답을 보냈어요. 주소가 바뀌었거나 로그인이 필요한 페이지인지 사이트를 직접 열어 확인해 주세요.",
  URL_NOT_ALLOWED: "접근이 허용되지 않은 주소로 이동하는 페이지예요. 사이트를 직접 열어 이동한 주소를 확인해 주세요.",
  NAVIGATION_TIMEOUT: "사이트가 제시간에 응답하지 않았어요. 잠시 뒤 다시 확인해 주세요.",
  TEXT_TOO_LARGE: "확인할 글자가 너무 많아요. 타워 수정에서 감시할 영역을 지정해 범위를 줄여 주세요.",
  NAVIGATION_FAILED: "사이트에 접속하지 못했어요. 사이트를 직접 열어 주소가 맞는지 확인해 주세요.",
  INVALID_SELECTOR: "감시할 영역의 CSS 선택자 형식이 올바르지 않아요. 타워 수정에서 선택자를 고쳐 주세요.",
  UNKNOWN: UNKNOWN_CRAWL_FAILURE
};

export function describeCrawlFailure(crawlerResultCode: CrawlerResultCode | null): string {
  return crawlerResultCode === null ? UNKNOWN_CRAWL_FAILURE : CRAWL_FAILURE_DESCRIPTIONS[crawlerResultCode];
}

export function isDetectionAreaSelectorValid(selector: string | null): boolean {
  if (selector === null) return true;
  try {
    document.createDocumentFragment().querySelector(selector);
    return true;
  } catch {
    return false;
  }
}

export function isConditionComplete(condition: WatchConditionInput): boolean {
  return condition.keyword.trim().length > 0;
}

/** 빈 칸이면 페이지 전체를 감시하므로 null 로 보냄 */
export function toDetectionAreaSelector(input: string): string | null {
  return input.trim().length === 0 ? null : input;
}

/** 텍스트와 이미지 감지를 모두 끄고 감지 조건도 없으면 알림이 가지 않아서 백엔드가 저장을 거부함 */
export function detectsChange(detection: WatchDetection, conditionCount: number): boolean {
  return detection.detectTextChange || detection.detectImageChange || conditionCount > 0;
}

export function intervalMinutes(duration: string): number {
  const match = /^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?$/.exec(duration);
  if (match === null) return Number.NaN;
  return Number(match[1] ?? 0) * 60 + Number(match[2] ?? 0) + Number(match[3] ?? 0) / 60;
}

export function formatFetchInterval(duration: string): string {
  const minutes = intervalMinutes(duration);
  return Number.isFinite(minutes) ? `${minutes}분` : duration;
}
