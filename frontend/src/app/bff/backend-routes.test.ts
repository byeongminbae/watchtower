import { NextRequest } from "next/server";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { GET, POST, PUT } from "./[...path]/route";
import { GET as member, PUT as updateMember } from "./member/[memberId]/route";
import { GET as discordAuthorization } from "./oauth2/authorization/discord/route";
import { GET as discordCallback } from "./oauth2/callback/discord/route";
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
  const body = { name: "타워", fetchInterval: "PT5M" };
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
  ["admin/members?page=1&size=10&query=prefix", "admin/members?page=1&size=10&query=prefix"],
  ["towers/7/conditions", "towers/7/conditions"],
  ["towers/7/snapshots", "towers/7/snapshots"],
  ["towers/7/snapshots/41/screenshot", "towers/7/snapshots/41/screenshot"]
])("BFF 경로 %s는 허용한 백엔드 경로로 전달할 것이다", async (path, expected) => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(response());
  // when
  await GET(new NextRequest(`http://front/bff/${path}`), { params: Promise.resolve({ path: path.split("?")[0].split("/") }) });
  // then
  const sent = fetch.mock.calls[0]?.[0];
  expect(sent instanceof Request ? sent.url : sent).toBe(`http://backend:8080/api/v1/${expected}`);
});
it("백엔드가 캐시 기간을 정한 스크린샷은 그 캐시 설정을 브라우저에 그대로 전달할 것이다", async () => {
  // given
  vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(new Uint8Array([1, 2, 3]),
    { headers: { "content-type": "image/png", "cache-control": "max-age=604800, private" } }));
  const path = "towers/7/snapshots/41/screenshot";
  // when
  const result = await GET(new NextRequest(`http://front/bff/${path}`), { params: Promise.resolve({ path: path.split("/") }) });
  // then
  expect(result.headers.get("cache-control")).toBe("max-age=604800, private");
  expect(result.headers.get("content-type")).toBe("image/png");
  expect(new Uint8Array(await result.arrayBuffer())).toEqual(new Uint8Array([1, 2, 3]));
});
it("크롤링 재시도는 POST 로만 백엔드에 전달할 것이다", async () => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(response());
  const context = { params: Promise.resolve({ path: ["towers", "7", "crawl-retry"] }) };
  // when
  await POST(new NextRequest("http://front/bff/towers/7/crawl-retry", { method: "POST" }), context);
  const rejected = await GET(new NextRequest("http://front/bff/towers/7/crawl-retry"), context);
  // then
  const sent = fetch.mock.calls[0]?.[0];
  expect(sent instanceof Request ? sent.url : sent).toBe("http://backend:8080/api/v1/towers/7/crawl-retry");
  expect(sent instanceof Request ? sent.method : undefined).toBe("POST");
  expect(rejected.status).toBe(405);
  expect(fetch).toHaveBeenCalledTimes(1);
});
it.each(["pause", "resume"])("타워 %s 요청은 POST 로만 백엔드에 전달할 것이다", async action => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(response());
  const context = { params: Promise.resolve({ path: ["towers", "7", action] }) };
  // when
  await POST(new NextRequest(`http://front/bff/towers/7/${action}`, { method: "POST" }), context);
  const rejected = await GET(new NextRequest(`http://front/bff/towers/7/${action}`), context);
  // then
  const sent = fetch.mock.calls[0]?.[0];
  expect(sent instanceof Request ? sent.url : sent).toBe(`http://backend:8080/api/v1/towers/7/${action}`);
  expect(sent instanceof Request ? sent.method : undefined).toBe("POST");
  expect(rejected.status).toBe(405);
  expect(fetch).toHaveBeenCalledTimes(1);
});
it("회원 테스트 알림 요청은 POST 로만 백엔드에 전달할 것이다", async () => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(response());
  const context = { params: Promise.resolve({ path: ["member", "7", "test-notification"] }) };
  // when
  await POST(new NextRequest("http://front/bff/member/7/test-notification", { method: "POST" }), context);
  const rejected = await GET(new NextRequest("http://front/bff/member/7/test-notification"), context);
  // then
  const sent = fetch.mock.calls[0]?.[0];
  expect(sent instanceof Request ? sent.url : sent).toBe("http://backend:8080/api/v1/member/7/test-notification");
  expect(sent instanceof Request ? sent.method : undefined).toBe("POST");
  expect(rejected.status).toBe(405);
  expect(fetch).toHaveBeenCalledTimes(1);
});
it("타워 테스트 알림 요청은 더 이상 백엔드에 전달하지 않을 것이다", async () => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(response());
  // when
  const rejected = await POST(new NextRequest("http://front/bff/towers/7/notify", { method: "POST" }), { params: Promise.resolve({ path: ["towers", "7", "notify"] }) });
  // then
  expect(rejected.status).toBe(404);
  expect(fetch).not.toHaveBeenCalled();
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
it("Discord 로그인 시작은 백엔드의 Discord 이동 응답과 세션 쿠키를 그대로 돌려줄 것이다", async () => {
  // given
  const headers = new Headers({ location: "https://discord.com/oauth2/authorize?state=fixture" });
  headers.append("set-cookie", "JSESSIONID=login; Path=/; HttpOnly");
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 302, headers }));
  // when
  const result = await discordAuthorization(new NextRequest("http://front/bff/oauth2/authorization/discord?next=/admin"));
  // then
  const sent = fetch.mock.calls[0]?.[0];
  expect(sent instanceof Request ? sent.url : null).toBe("http://backend:8080/api/v1/oauth2/authorization/discord");
  expect(result.status).toBe(302);
  expect(result.headers.get("location")).toBe("https://discord.com/oauth2/authorize?state=fixture");
  expect(result.headers.getSetCookie()).toEqual(["JSESSIONID=login; Path=/; HttpOnly"]);
});
it("Discord 콜백은 인가 응답 값과 세션 쿠키만 백엔드에 전달할 것이다", async () => {
  // given
  const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 302, headers: { location: "http://front/login/callback" } }));
  // when
  const result = await discordCallback(new NextRequest("http://front/bff/oauth2/callback/discord?code=a%20b&state=s&extra=x", { headers: { cookie: "JSESSIONID=login" } }));
  // then
  const sent = fetch.mock.calls[0]?.[0];
  expect(sent instanceof Request ? sent.url : null).toBe("http://backend:8080/api/v1/oauth2/callback/discord?code=a+b&state=s");
  expect(sent instanceof Request ? sent.headers.get("cookie") : null).toBe("JSESSIONID=login");
  expect(result.headers.get("location")).toBe("http://front/login/callback");
});
it("백엔드가 로그인 이동 응답을 주지 못하면 로그인 확인 화면으로 오류와 함께 보낼 것이다", async () => {
  // given
  vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json({ isSuccess: false }, { status: 500 }));
  // when
  const result = await discordCallback(new NextRequest("http://front/bff/oauth2/callback/discord?code=c&state=s"));
  // then
  expect(result.status).toBe(303);
  expect(result.headers.get("location")).toBe("/login/callback?error=server_error");
});
it.each(["admin/secrets", "auth/renew", "towers/../internal", "notification/channels"])("허용하지 않는 API %s는 백엔드에 전달하지 않을 것이다", async path => {
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
