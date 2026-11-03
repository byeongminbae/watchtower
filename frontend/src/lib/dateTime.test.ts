import { describe, expect, it } from "vitest";
import { formatElapsedTime, formatRelativeTime } from "./dateTime";

describe("타워 활동 시간", () => {
  it("확인한 지 1분이 지나지 않으면 방금으로 표시할 것이다", () => {
    // given
    const now = Date.parse("2026-10-27T03:00:30Z");
    // when
    const result = formatRelativeTime("2026-10-27T03:00:00Z", now);
    // then
    expect(result).toBe("방금");
  });

  it("확인한 지 5분이 지나면 분 단위의 경과 시간을 표시할 것이다", () => {
    // given
    const now = Date.parse("2026-10-27T03:05:00Z");
    // when
    const result = formatRelativeTime("2026-10-27T03:00:00Z", now);
    // then
    expect(result).toBe("5분 전");
  });

  it("미래 시각을 받으면 경과 시간으로 오인하지 않고 남은 시간으로 표시할 것이다", () => {
    // given
    const now = Date.parse("2026-10-27T03:00:00Z");
    // when
    const result = formatRelativeTime("2026-10-27T03:05:00Z", now);
    // then
    expect(result).toBe("5분 후");
  });
});

describe("변경 감지 경과 시간", () => {
  it("오늘 1분이 지나지 않았으면 초 단위로 표시할 것이다", () => {
    // given
    const now = new Date(2026, 10, 4, 15, 0, 3).getTime();
    // when
    const result = formatElapsedTime(new Date(2026, 10, 4, 15, 0, 0).toISOString(), now);
    // then
    expect(result).toBe("3초 전");
  });

  it("오늘 1시간이 지나지 않았으면 분 단위로 표시할 것이다", () => {
    // given
    const now = new Date(2026, 10, 4, 15, 2, 30).getTime();
    // when
    const result = formatElapsedTime(new Date(2026, 10, 4, 15, 0, 0).toISOString(), now);
    // then
    expect(result).toBe("2분 전");
  });

  it("오늘 1시간 이상 지났으면 시간 단위로 표시할 것이다", () => {
    // given
    const now = new Date(2026, 10, 4, 15, 0, 0).getTime();
    // when
    const result = formatElapsedTime(new Date(2026, 10, 4, 0, 10, 0).toISOString(), now);
    // then
    expect(result).toBe("14시간 전");
  });

  it("24시간이 지나지 않았어도 날짜가 바뀌었으면 어제로 표시할 것이다", () => {
    // given
    const now = new Date(2026, 10, 4, 0, 5, 0).getTime();
    // when
    const result = formatElapsedTime(new Date(2026, 10, 3, 23, 50, 0).toISOString(), now);
    // then
    expect(result).toBe("어제");
  });

  it("이틀 이상 지났으면 며칠 전인지 표시할 것이다", () => {
    // given
    const now = new Date(2026, 10, 4, 9, 0, 0).getTime();
    // when
    const result = formatElapsedTime(new Date(2026, 10, 1, 20, 0, 0).toISOString(), now);
    // then
    expect(result).toBe("3일 전");
  });

  it("서버 시각이 브라우저 시계보다 조금 앞서도 남은 시간으로 표시하지 않을 것이다", () => {
    // given
    const now = new Date(2026, 10, 4, 15, 0, 0).getTime();
    // when
    const result = formatElapsedTime(new Date(2026, 10, 4, 15, 0, 2).toISOString(), now);
    // then
    expect(result).toBe("지금");
  });
});
