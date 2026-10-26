import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "./client";
import { clearSession, commitSession, readSession } from "../session";
import type { ApiEnvelope } from "./types";

const timestamp = "2026-10-26T12:00:00";
const csrf = { isSuccess: true, timestamp, data: { token: "csrf-fixture", headerName: "X-CSRF-TOKEN" } };
beforeEach(clearSession);
afterEach(() => vi.restoreAllMocks());

describe("API 응답 경계", () => {
  it("현재 성공 응답과 검증한 데이터만 반환할 것이다", async () => {
    // given
    const value = { isSuccess: true, data: "완료", timestamp };
    vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json(value));
    // when
    const result = await apiClient.get("/bff/test", (data): data is string => typeof data === "string");
    // then
    expect(result).toEqual(value);
  });
  it.each([
    { success: true, data: "이전 계약", timestamp },
    { isSuccess: true, timestamp },
    { isSuccess: true, data: null, timestamp },
    { isSuccess: false, statusCode: 401, message: "잘못된 오류", timestamp }
  ])("잘못된 응답 %j를 성공 데이터로 반환하지 않을 것이다", async value => {
    // given
    vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json(value));
    // when
    const request = apiClient.get("/bff/test", (data): data is string => typeof data === "string");
    // then
    await expect(request).rejects.toMatchObject({ code: "INVALID_RESPONSE" });
  });
  it.each([200, 404])("HTTP %s 업무 오류는 오류 코드와 메시지를 보존할 것이다", async status => {
    // given
    vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json({ isSuccess: false, statusCode: "TOW_001", message: "타워 없음", timestamp }, { status }));
    // when
    const request = apiClient.get<ApiEnvelope<string>>("/bff/test");
    // then
    await expect(request).rejects.toMatchObject({ status, code: "TOW_001", message: "타워 없음" });
  });
  it.each([["", "EMPTY_RESPONSE"], ["{broken", "MALFORMED_RESPONSE"]])("JSON 본문 %s가 유효하지 않으면 %s 오류가 발생할 것이다", async (body, code) => {
    // given
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(body));
    // when
    const request = apiClient.get<ApiEnvelope<string>>("/bff/test");
    // then
    await expect(request).rejects.toMatchObject({ code });
  });
  it("네트워크가 실패하면 재요청 없이 네트워크 오류를 반환할 것이다", async () => {
    // given
    const fetch = vi.spyOn(globalThis, "fetch").mockRejectedValue(new TypeError("연결 실패"));
    // when
    const request = apiClient.get<ApiEnvelope<string>>("/bff/test");
    // then
    await expect(request).rejects.toMatchObject({ code: "NETWORK_ERROR" });
    expect(fetch).toHaveBeenCalledOnce();
  });
  it("HTTP 401은 로그인 상태를 제거할 것이다", async () => {
    // given
    commitSession({ memberId: 7, role: "NORMAL" });
    vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json({}, { status: 401 }));
    // when
    const request = apiClient.get<ApiEnvelope<string>>("/bff/test");
    // then
    await expect(request).rejects.toMatchObject({ code: "UNAUTHORIZED" });
    expect(readSession()).toBeNull();
  });
  it.each(["/api/v1/towers", "https://backend.example/api/v1/towers", "//attacker.example/bff/test"])("허용하지 않는 경로 %s는 전송 전에 거부할 것이다", async path => {
    // given
    const fetch = vi.spyOn(globalThis, "fetch");
    // when
    const request = apiClient.get<ApiEnvelope<string>>(path);
    // then
    await expect(request).rejects.toMatchObject({ code: "INVALID_CLIENT_ROUTE" });
    expect(fetch).not.toHaveBeenCalled();
  });
});

it("변경 요청 전에 CSRF를 조회하고 세션 쿠키와 함께 필수 헤더를 전송할 것이다", async () => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(csrf))
    .mockResolvedValueOnce(Response.json({ isSuccess: true, timestamp }));
  // when
  await apiClient.postEmpty("/bff/towers", { name: "타워", fetchInterval: "PT5M" });
  // then
  expect(fetch).toHaveBeenNthCalledWith(1, "/bff/auth/csrf", expect.objectContaining({ method: "GET", credentials: "include" }));
  expect(fetch).toHaveBeenNthCalledWith(2, "/bff/towers", expect.objectContaining({
    method: "POST", credentials: "include", headers: { "Content-Type": "application/json", "X-CSRF-TOKEN": "csrf-fixture" },
    body: JSON.stringify({ name: "타워", fetchInterval: "PT5M" })
  }));
});
it("CSRF 조회에 실패하면 변경 요청을 보내지 않을 것이다", async () => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json({ isSuccess: false, statusCode: "AUT_001", message: "만료", timestamp }));
  // when
  const request = apiClient.deleteEmpty("/bff/towers/1");
  // then
  await expect(request).rejects.toMatchObject({ code: "AUT_001" });
  expect(fetch).toHaveBeenCalledOnce();
});
it("CSRF가 로그인 후 교체되면 다음 변경 요청에서 새 토큰을 사용할 것이다", async () => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(csrf)).mockResolvedValueOnce(Response.json({ isSuccess: true, timestamp }))
    .mockResolvedValueOnce(Response.json({ ...csrf, data: { ...csrf.data, token: "rotated" } })).mockResolvedValueOnce(Response.json({ isSuccess: true, timestamp }));
  // when
  await apiClient.postEmpty("/bff/towers", {});
  await apiClient.deleteEmpty("/bff/auth/logout");
  // then
  expect(fetch).toHaveBeenNthCalledWith(4, "/bff/auth/logout", expect.objectContaining({ headers: { "Content-Type": "application/json", "X-CSRF-TOKEN": "rotated" } }));
});
