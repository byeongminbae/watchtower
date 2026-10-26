import ky from "ky";
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

type ForwardOptions = {
  readonly method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  readonly searchParams?: URLSearchParams;
};
export const BACKEND_REQUEST_TIMEOUT_MS = 10_000;

export function bffError(status: number, statusCode: string, message: string): NextResponse {
  return NextResponse.json({ isSuccess: false, statusCode, message, timestamp: new Date().toISOString() }, { status });
}

export function invalidBffRequest(message: string): NextResponse {
  return bffError(400, "INVALID_BFF_REQUEST", message);
}

export function selectedSearchParams(request: NextRequest, names: readonly string[]): URLSearchParams {
  const selected = new URLSearchParams();
  for (const name of names) {
    const value = request.nextUrl.searchParams.get(name);
    if (value !== null) selected.set(name, value);
  }
  return selected;
}

export async function forwardBackendRequest(request: NextRequest, pathname: string, options: ForwardOptions = {}): Promise<Response> {
  const rawBaseUrl = process.env.INTERNAL_API_BASE_URL;
  if (rawBaseUrl === undefined || !URL.canParse(rawBaseUrl)) {
    return bffError(503, "FRONTEND_BACKEND_UNAVAILABLE", "서버에 연결할 수 없어요.");
  }
  const baseUrl = new URL(rawBaseUrl);
  if (!["http:", "https:"].includes(baseUrl.protocol) || baseUrl.username || baseUrl.password) {
    return bffError(503, "FRONTEND_BACKEND_UNAVAILABLE", "서버에 연결할 수 없어요.");
  }
  const url = new URL(pathname, baseUrl);
  url.search = (options.searchParams ?? request.nextUrl.searchParams).toString();
  const headers = new Headers();
  for (const name of ["accept", "cookie", "content-type", "x-csrf-token"]) {
    const value = request.headers.get(name);
    if (value !== null) headers.set(name, value);
  }
  const method = options.method ?? request.method;
  try {
    const upstream = await ky(url, {
      method, headers, cache: "no-store", retry: 0, redirect: "manual",
      body: ["GET", "HEAD"].includes(method) ? undefined : await request.text(),
      timeout: BACKEND_REQUEST_TIMEOUT_MS, throwHttpErrors: false
    });
    const responseHeaders = new Headers(upstream.headers);
    const cookies = upstream.headers.getSetCookie();
    for (const name of ["content-encoding", "content-length", "transfer-encoding", "set-cookie"]) responseHeaders.delete(name);
    responseHeaders.set("cache-control", "no-store");
    for (const cookie of cookies) responseHeaders.append("set-cookie", cookie);
    return new Response(upstream.body, { status: upstream.status, headers: responseHeaders });
  } catch (error) {
    if (!(error instanceof Error)) throw error;
    return bffError(502, "FRONTEND_BACKEND_REQUEST_FAILED", "서버 요청을 처리할 수 없어요.");
  }
}
