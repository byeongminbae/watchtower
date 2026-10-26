import { beforeEach, describe, expect, it, vi } from "vitest";
import { clearSession, commitSession, isSessionPrincipal, readSession, subscribeToSessionChanges } from "./session";

beforeEach(clearSession);
describe("서버 세션 상태", () => {
  it("서버가 반환한 회원과 권한을 저장하면 구독자에게 알려줄 것이다", () => {
    // given
    const listener = vi.fn();
    const unsubscribe = subscribeToSessionChanges(listener);
    // when
    const accepted = commitSession({ memberId: 7, role: "ADMIN" });
    // then
    expect(accepted).toBe(true);
    expect(readSession()).toEqual({ memberId: 7, role: "ADMIN" });
    expect(listener).toHaveBeenCalledOnce();
    unsubscribe();
  });
  it("로그아웃하면 회원 정보를 제거하고 구독자에게 알려줄 것이다", () => {
    // given
    commitSession({ memberId: 7, role: "NORMAL" });
    const listener = vi.fn();
    const unsubscribe = subscribeToSessionChanges(listener);
    // when
    clearSession();
    // then
    expect(readSession()).toBeNull();
    expect(listener).toHaveBeenCalledOnce();
    unsubscribe();
  });
  it("구독을 해제하면 이후 세션 변경 알림을 받지 않을 것이다", () => {
    // given
    const listener = vi.fn();
    subscribeToSessionChanges(listener)();
    // when
    commitSession({ memberId: 7, role: "NORMAL" });
    // then
    expect(listener).not.toHaveBeenCalled();
  });
  it.each([{ memberId: 0, role: "ADMIN" }, { memberId: 1, role: "ROOT" }, { role: "NORMAL" }, null, "fixture"])(
    "잘못된 세션 응답 %j는 로그인 정보로 사용할 수 없을 것이다", value => {
      // given
      const session = value;
      // when
      const valid = isSessionPrincipal(session);
      // then
      expect(valid).toBe(false);
    });
  it("브라우저에 JWT 쿠키를 위조해도 로그인 상태가 되지 않을 것이다", () => {
    // given
    document.cookie = "watchtower_jwt=forged; Path=/";
    // when
    const session = readSession();
    // then
    expect(session).toBeNull();
    document.cookie = "watchtower_jwt=; Max-Age=0; Path=/";
  });
});
