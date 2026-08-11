import ky from "ky";
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

type BackendMethod = "GET" | "POST" | "DELETE";

type ForwardOptions = {
  readonly method?: BackendMethod;
  readonly searchParams?: URLSearchParams;
};

const FORWARDED_REQUEST_HEADERS = [
  "accept",
  "authorization",
  "cookie",
] as const;

export const BACKEND_REQUEST_TIMEOUT_MS = 10_000;

function errorResponse(
  status: number,
  statusCode: string,
  message: string,
): NextResponse {
  return NextResponse.json(
    {
      success: false,
      statusCode,
      message,
      timestamp: new Date().toISOString(),
    },
    { status },
  );
}

function resolveBackendUrl(
  pathname: string,
  searchParams: URLSearchParams | undefined,
): URL | null {
  const rawBaseUrl = process.env.INTERNAL_API_BASE_URL;
  if (rawBaseUrl === undefined || !URL.canParse(rawBaseUrl)) return null;

  const baseUrl = new URL(rawBaseUrl);
  if (
    (baseUrl.protocol !== "http:" && baseUrl.protocol !== "https:") ||
    baseUrl.username !== "" ||
    baseUrl.password !== ""
  ) {
    return null;
  }

  const backendUrl = new URL(pathname, baseUrl);
  if (searchParams !== undefined) backendUrl.search = searchParams.toString();
  return backendUrl;
}

function forwardedHeaders(request: NextRequest): Headers {
  const headers = new Headers();
  for (const name of FORWARDED_REQUEST_HEADERS) {
    const value = request.headers.get(name);
    if (value !== null) headers.set(name, value);
  }
  return headers;
}

function relayResponse(upstream: Response): Response {
  const headers = new Headers(upstream.headers);
  headers.delete("content-encoding");
  headers.delete("content-length");
  headers.delete("transfer-encoding");
  return new Response(upstream.body, {
    status: upstream.status,
    statusText: upstream.statusText,
    headers,
  });
}

export function invalidBffRequest(message: string): NextResponse {
  return errorResponse(400, "INVALID_BFF_REQUEST", message);
}

export function selectedSearchParams(
  request: NextRequest,
  names: readonly string[],
): URLSearchParams {
  const selected = new URLSearchParams();
  for (const name of names) {
    const value = request.nextUrl.searchParams.get(name);
    if (value !== null) selected.set(name, value);
  }
  return selected;
}

export async function forwardBackendRequest(
  request: NextRequest,
  pathname: string,
  options: ForwardOptions = {},
): Promise<Response> {
  const backendUrl = resolveBackendUrl(pathname, options.searchParams);
  if (backendUrl === null) {
    return errorResponse(
      503,
      "FRONTEND_BACKEND_UNAVAILABLE",
      "백엔드 연결 설정을 사용할 수 없습니다.",
    );
  }

  try {
    const upstream = await ky(backendUrl, {
      method: options.method ?? request.method,
      headers: forwardedHeaders(request),
      cache: "no-store",
      retry: 0,
      timeout: BACKEND_REQUEST_TIMEOUT_MS,
      throwHttpErrors: false,
    });
    return relayResponse(upstream);
  } catch (error) {
    if (!(error instanceof Error)) throw error;
    return errorResponse(
      502,
      "FRONTEND_BACKEND_REQUEST_FAILED",
      "백엔드 요청을 처리할 수 없습니다.",
    );
  }
}
