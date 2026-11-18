import type { WatchConditionInput, WatchDetection } from "@/types/tower";

/** 백엔드 CssSelector.MAX_LENGTH 와 같은 값 */
export const MAX_DETECTION_AREA_SELECTOR_LENGTH = 500;

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
