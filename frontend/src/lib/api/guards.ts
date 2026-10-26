import type { Watch, WatchCondition, WatchSnapshot, HotUrl } from "@/types/tower";
import type { AdminMember, AuthStats, MemberStats, TowerStats } from "@/types/admin";
import type { CursorPage, OffsetPage } from "@/types/pagination";
import type { ApiDataGuard } from "./types";

function object(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
function id(value: unknown): value is number { return typeof value === "number" && Number.isSafeInteger(value) && value > 0; }
function count(value: unknown): value is number { return typeof value === "number" && Number.isSafeInteger(value) && value >= 0; }
function text(value: unknown): value is string | null { return value === null || typeof value === "string"; }
function httpUrl(value: unknown): value is string {
  return typeof value === "string" && URL.canParse(value) && ["http:", "https:"].includes(new URL(value).protocol);
}
export function isWatch(value: unknown): value is Watch {
  return object(value) && id(value.id) && typeof value.name === "string" && httpUrl(value.url) &&
    typeof value.fetchInterval === "string" && typeof value.includeInStat === "boolean" &&
    typeof value.status === "string" && ["RUNNING", "PAUSED", "ILLEGAL_SUSPENDED"].includes(value.status) &&
    text(value.faviconUrl) && text(value.lastFetchedAt) && text(value.lastNotifiedAt) &&
    (value.latestAiSummary === undefined || text(value.latestAiSummary));
}
export function isCondition(value: unknown): value is WatchCondition {
  if (!object(value) || !id(value.id)) return false;
  switch (value.type) {
    case "HTML": return true;
    case "KEYWORD": return typeof value.keyword === "string" && typeof value.isSatisfiedAtAppearance === "boolean";
    case "REGEX": return typeof value.regex === "string" && typeof value.isSatisfiedAtAppearance === "boolean";
    default: return false;
  }
}
export function isSnapshot(value: unknown): value is WatchSnapshot {
  return object(value) && id(value.id) && typeof value.htmlContentUrl === "string" &&
    text(value.htmlContentDiffUrl) && text(value.screenshotUrl) && text(value.screenshotDiffUrl) &&
    text(value.aiDiffSummary) && text(value.notifiedAt);
}
export function isHotUrl(value: unknown): value is HotUrl {
  return object(value) && typeof value.domain === "string" && httpUrl(value.url) && count(value.changeCount);
}
export function isAdminMember(value: unknown): value is AdminMember {
  return object(value) && id(value.id) && typeof value.nickname === "string" && typeof value.email === "string" &&
    typeof value.profileImageUrl === "string" && typeof value.lastSignInAt === "string" &&
    (value.role === "NORMAL" || value.role === "ADMIN") && text(value.bannedAt) && text(value.banReason) && text(value.deletedAt);
}
export function arrayOf<T>(guard: ApiDataGuard<T>): ApiDataGuard<T[]> {
  return (value: unknown): value is T[] => Array.isArray(value) && value.every(guard);
}
export function cursorOf<T>(guard: ApiDataGuard<T>): ApiDataGuard<CursorPage<T>> {
  return (value: unknown): value is CursorPage<T> => object(value) && Array.isArray(value.data) &&
    value.data.every(guard) && count(value.size) && typeof value.hasNext === "boolean" &&
    (value.nextCursorId === null || id(value.nextCursorId)) && text(value.nextCursorValue) &&
    (!value.hasNext || id(value.nextCursorId));
}
export function offsetOf<T>(guard: ApiDataGuard<T>): ApiDataGuard<OffsetPage<T>> {
  return (value: unknown): value is OffsetPage<T> => object(value) && Array.isArray(value.data) &&
    value.data.every(guard) && count(value.size) && count(value.page) && count(value.totalElements) && count(value.totalPages);
}
export function isTowerStats(value: unknown): value is TowerStats {
  return object(value) && count(value.totalCount) && count(value.runningCount) && count(value.pausedCount) && count(value.illegalSuspendedCount);
}
export function isMemberStats(value: unknown): value is MemberStats {
  return object(value) && count(value.totalCount) && count(value.activeCount) && count(value.bannedCount) && count(value.deletedCount);
}
export function isAuthStats(value: unknown): value is AuthStats {
  return object(value) && object(value.signUps) && object(value.signIns) &&
    count(value.signUps.last7Days) && count(value.signUps.last30Days) && count(value.signIns.last7Days) && count(value.signIns.last30Days);
}
