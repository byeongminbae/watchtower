export type WatchStatus = "READY" | "ERROR" | "RUNNING" | "PAUSED" | "SUSPENDED";
export type WatchConditionType = "KEYWORD";
export type PresenceChange = "APPEAR" | "DISAPPEAR";
export type WatchConditionInput = { readonly type: "KEYWORD"; readonly keyword: string; readonly satisfiedWhen: PresenceChange };
export type WatchCondition = WatchConditionInput & { readonly id: number };
/**
 * 감지 조건과 별개로 타워에 켜 두는 감지 방식
 * @property detectionAreaSelector 감시할 영역의 CSS 선택자. 지정하면 텍스트, 이미지, 감지 조건 모두 이 영역 안에서만 판정하고, null 이면 페이지 전체
 */
export interface WatchDetection {
  readonly detectTextChange: boolean;
  readonly detectImageChange: boolean;
  readonly ignoreDateTime: boolean;
  readonly detectionAreaSelector: string | null;
}
export interface Watch extends WatchDetection {
  readonly id: number;
  readonly name: string;
  readonly url: string;
  readonly faviconUrl: string | null;
  readonly status: WatchStatus;
  readonly fetchInterval: string;
  readonly includeInStat: boolean;
  readonly lastFetchedAt: string | null;
  readonly lastNotifiedAt: string | null;
  readonly latestAiSummary?: string | null;
}
export interface WatchSnapshot {
  readonly id: number;
  readonly detectedAt: string;
  readonly highlightedScreenshotUrl: string | null;
  readonly aiDiffSummary: string | null;
  readonly notifiedAt: string | null;
}
export interface HotUrl {
  readonly domain: string;
  readonly url: string;
  readonly detectionCount: number;
}
