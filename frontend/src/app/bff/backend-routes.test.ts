import { NextRequest } from "next/server";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { GET, POST, PUT } from "./[...path]/route";
import { GET as member, PUT as updateMember } from "./member/[memberId]/route";
import { GET as oauth } from "./auth/naver/url/route";
import { DELETE as logout } from "./auth/logout/route";
import { BACKEND_REQUEST_TIMEOUT_MS } from "@/lib/server/backendGateway";

beforeEach(() => vi.stubEnv("INTERNAL_API_BASE_URL", "http://backend:8080"));
afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks(); vi.unstubAllEnvs(); });
function response() {
  const headers = new Headers({ "content-type": "application/json" });
  headers.append("set-cookie", "JSESSIONID=rotated; Path=/; HttpOnly");
  headers.append("set-cookie", "another=fixture; Path=/");
  return Response.json({ isSuccess: true, timestamp: "2026-10-26", data: "fixture" }, { headers });
}
it("타워 생성 본문과 CSRF 및 세션 쿠키를 백엔드에 전달할 것이다", async () => {
  // given
  let sentBody: unknown;
  const fetch = vi.spyOn(globalThis, "fetch").mockImplementation(async input => {
    if (input instanceof Request) sentBody = await input.clone().json();
    return response();
  });
  const body = { name: "타워", fetchInterval: "PT5M", notificationChannelIds: [3] };
  const request = new NextRequest("http://front/bff/towers", { method: "POST",
    headers: { cookie: "JSESSIONID=fixture", "x-csrf-token": "csrf", "content-type": "application/json", authorization: "Bearer forged" },
    body: JSON.stringify(body) });
  // when
  const result = await POST(request, { params: Promise.resolve({ path: ["towers"] }) });
  // then
  const sent = fetch.mock.calls[0]?.[0];
  expect(sent).toBeInstanceOf(Request);
  if (!(sent instanceof Request)) throw new Error("요청 없음");
  expect(sent.url).toBe("http://backend:8080/api/v1/towers");
  expect(sent.headers.get("cookie")).toBe("JSESSIONID=fixture");
  expect(sent.headers.get("x-csrf-token")).toBe("csrf");
  expect(sent.headers.has("authorization")).toBe(false);
  expect(sentBody).toEqual(body);
  expect(result.headers.getSetCookie()).toHaveLength(2);
  expect(result.headers.get("cache-control")).toBe("no-store");
});
it.each([
  ["auth/session", "auth/session"],
  ["notification/channels?cursorId=3&cursorValue=abc&sortField=CREATED_AT", "notification/channels?cursorId=3&cursorValue=abc&sortField=CREATED_AT"],
  ["admin/members?page=1&size=10&query=prefix", "admin/members?page=1&size=10&query=prefix"],
  ["towers/7/conditions", "towers/7/conditions"],
  ["towers/7/snapshots", "towers/7/snapshots"]
])("BFF 경로 %s는 허용한 백엔드 경로로 전달할 것이다", async (path, expected) => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(response());
  // when
  await GET(new NextRequest(`http://front/bff/${path}`), { params: Promise.resolve({ path: path.split("?")[0].split("/") }) });
  // then
  const sent = fetch.mock.calls[0]?.[0];
  expect(sent instanceof Request ? sent.url : sent).toBe(`http://backend:8080/api/v1/${expected}`);
});
it("회원 수정은 PUT 본문을 전달할 것이다", async () => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(response());
  // when
  await updateMember(new NextRequest("http://front/bff/member/7", { method: "PUT", body: JSON.stringify({ nickname: "새 이름", profileImageUrl: "" }) }), { params: Promise.resolve({ memberId: "7" }) });
  // then
  const sent = fetch.mock.calls[0]?.[0];
  expect(sent instanceof Request ? sent.method : null).toBe("PUT");
});
it("네이버 로그인 URL 조회에는 CSRF 헤더를 보존할 것이다", async () => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(response());
  // when
  await oauth(new NextRequest("http://front/bff/auth/naver/url?state=a%20b", { headers: { "x-csrf-token": "csrf" } }));
  // then
  const sent = fetch.mock.calls[0]?.[0];
  expect(sent instanceof Request ? sent.headers.get("x-csrf-token") : null).toBe("csrf");
});
it.each(["admin/secrets", "auth/renew", "towers/../internal", "notification/channels/0"])("허용하지 않는 API %s는 백엔드에 전달하지 않을 것이다", async path => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch");
  // when
  const result = await GET(new NextRequest("http://front/bff/test"), { params: Promise.resolve({ path: path.split("/") }) });
  // then
  expect(result.status).toBe(404);
  expect(fetch).not.toHaveBeenCalled();
});
it("허용하지 않는 HTTP 메서드는 전달하지 않을 것이다", async () => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch");
  // when
  const result = await PUT(new NextRequest("http://front/bff/auth/session", { method: "PUT" }), { params: Promise.resolve({ path: ["auth", "session"] }) });
  // then
  expect(result.status).toBe(405);
  expect(fetch).not.toHaveBeenCalled();
});
it("잘못된 회원 식별자는 요청 전에 거부할 것이다", async () => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch");
  // when
  const result = await member(new NextRequest("http://front/bff/member/nope"), { params: Promise.resolve({ memberId: "nope" }) });
  // then
  expect(result.status).toBe(400);
  expect(fetch).not.toHaveBeenCalled();
});
it("연결 설정이 없으면 백엔드에 요청하지 않고 연결 오류를 반환할 것이다", async () => {
  // given
  vi.stubEnv("INTERNAL_API_BASE_URL", "");
  const fetch = vi.spyOn(globalThis, "fetch");
  // when
  const result = await logout(new NextRequest("http://front/bff/auth/logout", { method: "DELETE" }));
  // then
  expect(result.status).toBe(503);
  expect(fetch).not.toHaveBeenCalled();
});
it("백엔드 요청이 제한 시간 안에 끝나지 않으면 게이트웨이 오류를 반환할 것이다", async () => {
  // given
  vi.useFakeTimers();
  vi.spyOn(globalThis, "fetch").mockImplementation(input => {
    if (!(input instanceof Request)) throw new Error("요청 형식 오류");
    return new Promise((_resolve, reject) => input.signal.addEventListener("abort", () => reject(input.signal.reason), { once: true }));
  });
  const pending = logout(new NextRequest("http://front/bff/auth/logout", { method: "DELETE" }));
  // when
  await vi.advanceTimersByTimeAsync(BACKEND_REQUEST_TIMEOUT_MS + 1);
  const result = await pending;
  // then
  expect(result.status).toBe(502);
});
