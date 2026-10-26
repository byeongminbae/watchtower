export type WatchStatus = "RUNNING" | "PAUSED" | "ILLEGAL_SUSPENDED";
export type WatchConditionType = "KEYWORD" | "REGEX" | "HTML";
export type WatchConditionInput =
  | { readonly type: "HTML" }
  | { readonly type: "KEYWORD"; readonly keyword: string; readonly isSatisfiedAtAppearance: boolean }
  | { readonly type: "REGEX"; readonly regex: string; readonly isSatisfiedAtAppearance: boolean };
export type WatchCondition = WatchConditionInput & { readonly id: number };
export interface Watch {
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
  readonly htmlContentUrl: string;
  readonly htmlContentDiffUrl: string | null;
  readonly screenshotUrl: string | null;
  readonly screenshotDiffUrl: string | null;
  readonly aiDiffSummary: string | null;
  readonly notifiedAt: string | null;
}
export interface HotUrl {
  readonly domain: string;
  readonly url: string;
  readonly changeCount: number;
}
