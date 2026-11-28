import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import WatchStats from "./WatchStats";
import type { Watch } from "@/types/tower";
const watch: Watch = { id: 9, name: "굿글!", url: "https://google.com", faviconUrl: null, status: "READY", fetchInterval: "PT5M", includeInStat: true, detectTextChange: true, detectImageChange: false, ignoreDateTime: false, detectionAreaSelector: null, lastFetchedAt: "2026-11-03T21:00:00Z", lastCrawlerResultCode: null, lastNotifiedAt: null, latestAiSummary: null };
beforeEach(() => { vi.useFakeTimers(); vi.setSystemTime(new Date("2026-11-03T21:01:00Z")); });
afterEach(() => { vi.useRealTimers(); });
it("시간이 지나면 새로고침 없이 확인 주기에 다음 확인까지 남은 시간을 줄여 보여줄 것이다", async () => {
  // given
  render(<WatchStats watch={watch} conditionCount={0} onOpenConditions={vi.fn()} />);
  const before = screen.getByText("5분마다").getAttribute("title");
  // when
  await act(async () => { await vi.advanceTimersByTimeAsync(60_000); });
  // then
  expect(before).toBe("다음 확인까지 약 4분");
  expect(screen.getByText("5분마다")).toHaveAttribute("title", "다음 확인까지 약 3분");
});
it("백그라운드 탭에서 타이머가 멈췄어도 탭이 다시 보이면 바로 현재 시각으로 최근 확인을 표시할 것이다", () => {
  // given
  render(<WatchStats watch={watch} conditionCount={0} onOpenConditions={vi.fn()} />);
  vi.setSystemTime(new Date("2026-11-03T21:05:03Z"));
  // when
  act(() => { document.dispatchEvent(new Event("visibilitychange")); });
  // then
  expect(screen.getByText("5분 전")).toBeVisible();
});
it("감지 조건 개수를 누르면 조건 관리 창을 열도록 알릴 것이다", () => {
  // given
  const onOpenConditions = vi.fn();
  render(<WatchStats watch={watch} conditionCount={2} onOpenConditions={onOpenConditions} />);
  // when
  fireEvent.click(screen.getByRole("button", { name: "키워드 2개 감지 조건 관리" }));
  // then
  expect(onOpenConditions).toHaveBeenCalledTimes(1);
});
