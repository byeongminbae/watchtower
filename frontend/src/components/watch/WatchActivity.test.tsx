import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import WatchActivity from "./WatchActivity";
import type { Watch } from "@/types/tower";
const watch: Watch = { id: 9, name: "굿글!", url: "https://google.com", faviconUrl: null, status: "CRAWLING_READY", fetchInterval: "PT5M", includeInStat: true, lastFetchedAt: "2026-11-03T21:00:00Z", lastNotifiedAt: null };
beforeEach(() => { vi.useFakeTimers(); vi.setSystemTime(new Date("2026-11-03T21:01:00Z")); });
afterEach(() => { vi.useRealTimers(); });
function progressValue() { return Number(screen.getByRole("progressbar", { name: "다음 확인까지 진행" }).getAttribute("aria-valuenow")); }
it("시간이 지나면 새로고침 없이 다음 확인까지 진행 표시가 늘어날 것이다", async () => {
  // given
  render(<WatchActivity watch={watch} />);
  const before = progressValue();
  // when
  await act(async () => { await vi.advanceTimersByTimeAsync(60_000); });
  // then
  expect(before).toBe(20);
  expect(progressValue()).toBe(40);
});
it("백그라운드 탭에서 타이머가 멈췄어도 탭이 다시 보이면 바로 현재 시각으로 최근 확인을 표시할 것이다", () => {
  // given
  render(<WatchActivity watch={watch} />);
  vi.setSystemTime(new Date("2026-11-03T21:05:03Z"));
  // when
  act(() => { document.dispatchEvent(new Event("visibilitychange")); });
  // then
  expect(screen.getByText("5분 전")).toBeVisible();
});
it("최근 확인 시각이 새로 바뀌면 진행 표시를 처음부터 다시 채울 것이다", () => {
  // given
  const { rerender } = render(<WatchActivity watch={{ ...watch, lastFetchedAt: "2026-11-03T20:50:00Z" }} />);
  const overdue = progressValue();
  // when
  rerender(<WatchActivity watch={watch} />);
  // then
  expect(overdue).toBe(100);
  expect(progressValue()).toBe(20);
});
