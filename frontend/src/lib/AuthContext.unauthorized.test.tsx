import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { apiClient } from "./api/client";
import { clearSession, commitSession, readSession } from "./session";
import type { ApiEnvelope } from "./api/types";

beforeEach(() => { clearSession(); commitSession({ memberId: 7, role: "ADMIN" }); });
afterEach(() => { vi.restoreAllMocks(); clearSession(); });
it.each(["AUT_001", "AUT_006", "AUT_007", "AUT_008"])("HTTP 200 응답의 %s 오류도 로그인 상태를 종료할 것이다", async code => {
  // given
  vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json({ isSuccess: false, statusCode: code, message: "인증 종료", timestamp: "2026-10-26" }));
  // when
  const request = apiClient.get<ApiEnvelope<string>>("/bff/test");
  // then
  await expect(request).rejects.toMatchObject({ code });
  expect(readSession()).toBeNull();
});
it("권한 부족 오류가 발생해도 정상 로그인한 회원의 세션은 유지할 것이다", async () => {
  // given
  vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json({ isSuccess: false, statusCode: "AUT_002", message: "권한 부족", timestamp: "2026-10-26" }));
  // when
  const request = apiClient.get<ApiEnvelope<string>>("/bff/test");
  // then
  await expect(request).rejects.toMatchObject({ code: "AUT_002" });
  expect(readSession()?.memberId).toBe(7);
});
it("새 로그인이 완료된 뒤 이전 요청이 인증 오류로 끝나도 새 세션은 유지할 것이다", async () => {
  // given
  let resolve: (value: Response) => void = () => undefined;
  vi.spyOn(globalThis, "fetch").mockReturnValue(new Promise(done => { resolve = done; }));
  const request = apiClient.get<ApiEnvelope<string>>("/bff/test");
  // when
  commitSession({ memberId: 8, role: "NORMAL" });
  resolve(Response.json({ isSuccess: false, statusCode: "AUT_001", message: "이전 인증 만료", timestamp: "2026-10-26" }));
  // then
  await expect(request).rejects.toMatchObject({ code: "AUT_001" });
  expect(readSession()?.memberId).toBe(8);
});
