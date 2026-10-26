import { afterEach, expect, it, vi } from "vitest";
import { authApi } from "./auth";

const timestamp = "2026-10-26T12:00:00";
afterEach(() => vi.restoreAllMocks());
it("네이버 로그인 URL을 가져올 때 GET 요청에도 CSRF를 전송할 것이다", async () => {
  // given
  const provider = "https://nid.naver.com/oauth2.0/authorize?state=fixture";
  const fetch = vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json({ isSuccess: true, timestamp, data: { token: "csrf", headerName: "X-CSRF-TOKEN" } }))
    .mockResolvedValueOnce(Response.json({ isSuccess: true, timestamp, data: provider }));
  // when
  const result = await authApi.getNaverLoginUrl("state /+");
  // then
  expect(result.data).toBe(provider);
  expect(fetch).toHaveBeenNthCalledWith(2, "/bff/auth/naver/url?state=state%20%2F%2B", expect.objectContaining({ headers: { "Content-Type": "application/json", "X-CSRF-TOKEN": "csrf" } }));
});
it.each(["https://attacker.example/", "http://nid.naver.com/oauth2.0/authorize", "https://nid.naver.com.evil.example/oauth2.0/authorize", "https://nid.naver.com/oauth2.0/token", "not a URL"])("공급자 URL %s가 허용한 주소가 아니면 거부할 것이다", async provider => {
  // given
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json({ isSuccess: true, timestamp, data: { token: "csrf", headerName: "X-CSRF-TOKEN" } }))
    .mockResolvedValueOnce(Response.json({ isSuccess: true, timestamp, data: provider }));
  // when
  const request = authApi.getNaverLoginUrl("fixture");
  // then
  await expect(request).rejects.toMatchObject({ code: "INVALID_RESPONSE" });
});
it("OAuth 콜백은 JWT 대신 회원 식별자와 권한을 받을 것이다", async () => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json({ isSuccess: true, timestamp, data: { memberId: 7, role: "ADMIN" } }));
  // when
  const result = await authApi.loginWithNaverCallback("code /+", "state /+");
  // then
  expect(result.data).toEqual({ memberId: 7, role: "ADMIN" });
  expect(fetch).toHaveBeenCalledWith("/bff/auth/naver/callback?code=code%20%2F%2B&state=state%20%2F%2B", expect.anything());
});
it.each([{ accessToken: "legacy", refreshToken: "legacy" }, { memberId: 7, role: "ROOT" }])("세션 응답 %j가 현재 계약과 다르면 거부할 것이다", async data => {
  // given
  vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json({ isSuccess: true, timestamp, data }));
  // when
  const request = authApi.getSession();
  // then
  await expect(request).rejects.toMatchObject({ code: "INVALID_RESPONSE" });
});
