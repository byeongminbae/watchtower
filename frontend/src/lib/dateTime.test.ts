import { describe, expect, it } from "vitest";
import { formatRelativeTime, toDate } from "./dateTime";

describe("타워 활동 시간", () => {
  it("시간대가 없는 서버 시각은 한국 시간으로 해석할 것이다", () => {
    // given
    const value = "2026-10-27T12:00:00";
    // when
    const date = toDate(value);
    // then
    expect(date.toISOString()).toBe("2026-10-27T03:00:00.000Z");
  });

  it("시간대가 있는 서버 시각은 다시 보정하지 않을 것이다", () => {
    // given
    const value = "2026-10-27T03:00:00Z";
    // when
    const date = toDate(value);
    // then
    expect(date.toISOString()).toBe("2026-10-27T03:00:00.000Z");
  });

  it("확인한 지 1분이 지나지 않으면 방금으로 표시할 것이다", () => {
    // given
    const now = Date.parse("2026-10-27T03:00:30Z");
    // when
    const result = formatRelativeTime("2026-10-27T12:00:00", now);
    // then
    expect(result).toBe("방금");
  });

  it("확인한 지 5분이 지나면 분 단위의 경과 시간을 표시할 것이다", () => {
    // given
    const now = Date.parse("2026-10-27T03:05:00Z");
    // when
    const result = formatRelativeTime("2026-10-27T12:00:00", now);
    // then
    expect(result).toBe("5분 전");
  });

  it("미래 시각을 받으면 경과 시간으로 오인하지 않고 남은 시간으로 표시할 것이다", () => {
    // given
    const now = Date.parse("2026-10-27T03:00:00Z");
    // when
    const result = formatRelativeTime("2026-10-27T12:05:00", now);
    // then
    expect(result).toBe("5분 후");
  });
});
