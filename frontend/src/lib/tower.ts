import type { WatchConditionInput } from "@/types/tower";

export function isConditionComplete(condition: WatchConditionInput): boolean {
  switch (condition.type) {
    case "HTML": return true;
    case "KEYWORD": return condition.keyword.trim().length > 0;
    case "REGEX": return condition.regex.trim().length > 0;
    default: { const exhaustive: never = condition; return exhaustive; }
  }
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
