import { act, render } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import WatchDetailPanel from "./WatchDetailPanel";
import type { Watch } from "@/types/tower";
const api = vi.hoisted(() => ({ getWatch: vi.fn(), getWatchConditions: vi.fn(), listWatchSnapshots: vi.fn() }));
vi.mock("@/lib/api", () => ({ watchApi: api }));
const watch: Watch = { id: 9, name: "굿글!", url: "https://google.com", faviconUrl: null, status: "CRAWLING_READY", fetchInterval: "PT5M", includeInStat: true, lastFetchedAt: "2026-11-03T20:55:38", lastNotifiedAt: null };
beforeEach(() => {
  vi.useFakeTimers();
  api.getWatch.mockReset(); api.getWatchConditions.mockResolvedValue({ data: [] }); api.listWatchSnapshots.mockResolvedValue({ data: [] });
  Object.defineProperty(document, "visibilityState", { configurable: true, value: "visible" });
});
afterEach(() => { vi.useRealTimers(); });
async function advance(millis: number) { await act(async () => { await vi.advanceTimersByTimeAsync(millis); }); }
it("크롤링 대기 중인 타워를 서버가 크롤링하기 시작하면 새로고침 없이 화면을 다시 불러올 것이다", async () => {
  // given
  const onChanged = vi.fn();
  api.getWatch.mockResolvedValue({ data: { ...watch, status: "CRAWLING" } });
  render(<WatchDetailPanel watch={watch} onChanged={onChanged} />);
  // when
  await advance(30_000);
  // then
  expect(api.getWatch).toHaveBeenCalledWith(9);
  expect(onChanged).toHaveBeenCalledTimes(1);
});
it("크롤링 중인 타워의 확인 시각이 바뀌면 화면을 다시 불러올 것이다", async () => {
  // given
  const onChanged = vi.fn();
  api.getWatch.mockResolvedValue({ data: { ...watch, status: "CRAWLING_READY", lastFetchedAt: "2026-11-03T21:01:02" } });
  render(<WatchDetailPanel watch={{ ...watch, status: "CRAWLING" }} onChanged={onChanged} />);
  // when
  await advance(5_000);
  // then
  expect(onChanged).toHaveBeenCalledTimes(1);
});
it("서버의 상태와 확인 시각이 화면과 같으면 화면을 다시 불러오지 않을 것이다", async () => {
  // given
  const onChanged = vi.fn();
  api.getWatch.mockResolvedValue({ data: watch });
  render(<WatchDetailPanel watch={watch} onChanged={onChanged} />);
  // when
  await advance(30_000);
  // then
  expect(api.getWatch).toHaveBeenCalledTimes(1);
  expect(onChanged).not.toHaveBeenCalled();
});
it("탭이 보이지 않는 동안에는 서버에 다시 확인하지 않을 것이다", async () => {
  // given
  Object.defineProperty(document, "visibilityState", { configurable: true, value: "hidden" });
  render(<WatchDetailPanel watch={watch} onChanged={vi.fn()} />);
  // when
  await advance(60_000);
  // then
  expect(api.getWatch).not.toHaveBeenCalled();
});
it("일시정지한 타워는 서버에 다시 확인하지 않을 것이다", async () => {
  // given
  render(<WatchDetailPanel watch={{ ...watch, status: "USER_PAUSED" }} onChanged={vi.fn()} />);
  // when
  await advance(60_000);
  // then
  expect(api.getWatch).not.toHaveBeenCalled();
});
