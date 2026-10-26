import { NextRequest } from "next/server";
import { afterEach, expect, it, vi } from "vitest";
import { proxy } from "./proxy";
const readSession = vi.hoisted(() => vi.fn());
vi.mock("@/lib/server/session", () => ({ readBackendSession: readSession }));
afterEach(() => vi.resetAllMocks());
it("세션 쿠키가 없으면 로그인 화면을 그대로 표시할 것이다", async () => {
  // given
  const request = new NextRequest("http://front/login");
  // when
  const response = await proxy(request);
  // then
  expect(response.headers.get("location")).toBeNull();
  expect(readSession).not.toHaveBeenCalled();
});
it.each(["NORMAL", "ADMIN"])("서버가 확인한 %s 회원은 로그인 화면에서 현재 화면으로 이동할 것이다", async role => {
  // given
  readSession.mockResolvedValue({ kind: "authenticated", principal: { memberId: 7, role } });
  // when
  const response = await proxy(new NextRequest("http://front/login", { headers: { cookie: "JSESSIONID=fixture" } }));
  // then
  expect(response.headers.get("location")).toBe(`http://front/${role === "ADMIN" ? "admin" : "watches"}`);
});
it.each(["anonymous", "unavailable"])("서버 세션 상태가 %s이면 쿠키만으로 로그인했다고 판단하지 않을 것이다", async kind => {
  // given
  readSession.mockResolvedValue({ kind });
  // when
  const response = await proxy(new NextRequest("http://front/login", { headers: { cookie: "JSESSIONID=forged" } }));
  // then
  expect(response.headers.get("location")).toBeNull();
});
