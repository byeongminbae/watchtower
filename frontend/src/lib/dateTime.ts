export function formatDateTime(value: string): string {
  return new Date(value).toLocaleString("ko-KR");
}

export function formatRelativeTime(value: string, now: number): string {
  const seconds = (new Date(value).getTime() - now) / 1000;
  if (Math.abs(seconds) < 60) return "방금";
  const formatter = new Intl.RelativeTimeFormat("ko-KR", { numeric: "auto" });
  for (const [unit, duration] of [["year", 31_536_000], ["month", 2_592_000], ["day", 86_400], ["hour", 3_600], ["minute", 60]] as const) {
    if (Math.abs(seconds) >= duration) return formatter.format(Math.trunc(seconds / duration), unit);
  }
  return "방금";
}

function startOfLocalDay(time: number): number {
  const date = new Date(time);
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}

/** 오늘이면 초, 분, 시간 단위의 경과 시간, 어제면 어제, 그 이전이면 며칠 전인지 표시함. 날짜는 브라우저 시간대 기준 */
export function formatElapsedTime(value: string, now: number): string {
  const time = Math.min(new Date(value).getTime(), now);
  const formatter = new Intl.RelativeTimeFormat("ko-KR", { numeric: "auto" });
  const dayDifference = Math.round((startOfLocalDay(now) - startOfLocalDay(time)) / 86_400_000);
  if (dayDifference > 0) return formatter.format(-dayDifference, "day");

  const seconds = Math.floor((now - time) / 1000);
  if (seconds < 60) return formatter.format(-seconds, "second");
  if (seconds < 3_600) return formatter.format(-Math.floor(seconds / 60), "minute");
  return formatter.format(-Math.floor(seconds / 3_600), "hour");
}
