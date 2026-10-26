import { beforeEach, expect, it } from "vitest";
import { clearSession, commitSession, readSession } from "./session";

beforeEach(clearSession);
it("새 세션 정보를 받으면 이전 권한을 대체할 것이다", () => {
  // given
  commitSession({ memberId: 7, role: "ADMIN" });
  // when
  commitSession({ memberId: 7, role: "NORMAL" });
  // then
  expect(readSession()).toEqual({ memberId: 7, role: "NORMAL" });
});
it("서버 세션을 받아도 브라우저에서 세션 쿠키를 작성하지 않을 것이다", () => {
  // given
  const before = document.cookie;
  // when
  commitSession({ memberId: 7, role: "NORMAL" });
  // then
  expect(document.cookie).toBe(before);
});
