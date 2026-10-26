import { describe, expect, it } from "vitest";
import { arrayOf, cursorOf, isCondition, isHotUrl, isWatch, offsetOf, isAdminMember } from "./guards";

describe("백엔드 응답 계약 검사", () => {
  it("이전 초 단위 주기와 결제 상태는 현재 타워 응답으로 받지 않을 것이다", () => {
    // given
    const legacy = { id: 1, name: "이전 타워", url: "https://example.com", status: "PAYMENT_REQUIRED", fetchIntervalSeconds: 300 };
    // when
    const valid = isWatch(legacy);
    // then
    expect(valid).toBe(false);
  });
  it("키워드 조건에 등장과 소멸 방향이 없으면 응답을 거부할 것이다", () => {
    // given
    const legacy = { id: 1, type: "KEYWORD", keyword: "마감" };
    // when
    const valid = isCondition(legacy);
    // then
    expect(valid).toBe(false);
  });
  it("다음 페이지가 있는데 커서가 없으면 커서 페이지를 거부할 것이다", () => {
    // given
    const page = { data: [], size: 20, hasNext: true, nextCursorId: null, nextCursorValue: null };
    // when
    const valid = cursorOf(isWatch)(page);
    // then
    expect(valid).toBe(false);
  });
  it("인기 URL 응답이 배열이 아니거나 변경 횟수가 없으면 거부할 것이다", () => {
    // given
    const legacy = [{ url: "https://example.com", hit: 12, statDate: "2026-10-26" }];
    // when
    const valid = arrayOf(isHotUrl)(legacy);
    // then
    expect(valid).toBe(false);
  });
  it("관리자 회원 응답은 탈퇴일과 정지 사유를 포함해 검사할 것이다", () => {
    // given
    const member = { id: 7, nickname: "회원", email: "member@example.com", profileImageUrl: "", lastSignInAt: "2026-10-26T12:00:00",
      role: "NORMAL", bannedAt: null, banReason: null, deletedAt: "2026-10-26T12:00:00" };
    // when
    const valid = offsetOf(isAdminMember)({ data: [member], page: 0, size: 20, totalElements: 1, totalPages: 1 });
    // then
    expect(valid).toBe(true);
  });
});
