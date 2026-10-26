import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { readBackendSession, requireServerSession } from "./session";

const mocks = vi.hoisted(() => ({ redirect: vi.fn((path: string): never => { throw new Error(`redirect:${path}`); }) }));
vi.mock("next/headers", () => ({ headers: async () => new Headers({ cookie: "JSESSIONID=fixture" }) }));
vi.mock("next/navigation", () => ({ redirect: mocks.redirect }));
beforeEach(() => vi.stubEnv("INTERNAL_API_BASE_URL", "http://backend:8080"));
afterEach(() => { vi.restoreAllMocks(); vi.unstubAllEnvs(); mocks.redirect.mockClear(); });
it("관리자 페이지의 서버 검사는 일반 회원을 관리자 화면에 진입시키지 않을 것이다", async () => {
  // given
  vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json({ isSuccess: true, data: { memberId: 7, role: "NORMAL" }, timestamp: "2026-10-26" }));
  // when
  const pending = requireServerSession(true);
  // then
  await expect(pending).rejects.toThrow("redirect:/watches");
});
it("위조된 쿠키라도 서버가 인증을 거부하면 로그인 화면으로 이동할 것이다", async () => {
  // given
  vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json({ isSuccess: false, statusCode: "AUT_001", message: "인증 필요" }));
  // when
  const pending = requireServerSession(true);
  // then
  await expect(pending).rejects.toThrow("redirect:/login?next=%2Fwatches");
});
it("서버가 관리자라고 확인한 경우에만 관리자 페이지 접근을 허용할 것이다", async () => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json({ isSuccess: true, data: { memberId: 7, role: "ADMIN" }, timestamp: "2026-10-26" }));
  // when
  const session = await requireServerSession(true);
  // then
  expect(session).toEqual({ memberId: 7, role: "ADMIN" });
  const sent = fetch.mock.calls[0]?.[0];
  expect(sent instanceof Request ? sent.headers.get("cookie") : null).toBe("JSESSIONID=fixture");
});
it("세션 서버가 응답하지 않으면 비로그인으로 잘못 분류하지 않을 것이다", async () => {
  // given
  vi.spyOn(globalThis, "fetch").mockRejectedValue(new TypeError("연결 실패"));
  // when
  const result = await readBackendSession("JSESSIONID=fixture");
  // then
  expect(result).toEqual({ kind: "unavailable" });
  expect(mocks.redirect).not.toHaveBeenCalled();
});
