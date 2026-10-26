export function toDate(value: string): Date {
  return new Date(/(?:Z|[+-]\d{2}:\d{2})$/i.test(value) ? value : `${value}+09:00`);
}

export function formatDateTime(value: string): string {
  return toDate(value).toLocaleString("ko-KR", { timeZone: "Asia/Seoul" });
}

export function formatRelativeTime(value: string, now: number): string {
  const seconds = (toDate(value).getTime() - now) / 1000;
  if (Math.abs(seconds) < 60) return "방금";
  const formatter = new Intl.RelativeTimeFormat("ko-KR", { numeric: "auto" });
  for (const [unit, duration] of [["year", 31_536_000], ["month", 2_592_000], ["day", 86_400], ["hour", 3_600], ["minute", 60]] as const) {
    if (Math.abs(seconds) >= duration) return formatter.format(Math.trunc(seconds / duration), unit);
  }
  return "방금";
}
