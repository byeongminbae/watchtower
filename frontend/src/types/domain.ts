// 백엔드 도메인 엔티티에 대응하는 프론트엔드 타입.
// domain: member, watch, subscription, payment 구조를 그대로 따름.

export type MemberRole = "NORMAL" | "ADMIN";

export interface Member {
  id: number;
  nickname: string;
  email: string;
  profileImageUrl: string;
  role?: MemberRole;
  lastSignInAt: string;
  isBanned?: boolean;
}

export type WatchStatus = "RUNNING" | "PAUSED" | "PAYMENT_REQUIRED" | "ILLEGAL_SUSPENDED";

export type WatchConditionType = "KEYWORD" | "REGEX" | "HTML_FULL";

export interface WatchCondition {
  id: number;
  type: WatchConditionType;
  keyword?: string;
  regex?: string;
}

export interface Watch {
  id: number;
  name: string;
  url: string;
  faviconUrl?: string;
  status: WatchStatus;
  intervalSeconds: number;
  includeInStat: boolean;
  lastFetchedAt?: string;
  lastNotifiedAt?: string;
  conditions?: WatchCondition[];
  latestAiSummary?: string;
}

export interface WatchSnapshot {
  id: number;
  watchId: number;
  htmlContentUrl: string;
  htmlContentDiffUrl: string;
  screenshotUrl: string;
  screenshotDiffUrl: string;
  aiDiffSummary: string;
  notifiedAt: string;
  createdAt: string;
}

export interface HotUrl {
  url: string;
  hit: number;
  statDate: string;
}

export type PlanTier = "FREE" | "BASIC" | "PRO";

export interface Plan {
  id: number;
  name: string;
  price: number;
  durationDays: number;
  planTier: PlanTier;
  maxWatchCount: number;
}

export interface Subscription {
  id: number;
  member: Member;
  currentPlan: Plan;
  startedAt: string;
  expiredAt: string;
}

export interface PaymentHistory {
  id: number;
  planName: string;
  planPrice: number;
  planDurationDays: number;
  planTier: PlanTier;
  createdAt: string;
}

export interface ApiResponse<T> {
  data: T;
  timestamp: string;
}
