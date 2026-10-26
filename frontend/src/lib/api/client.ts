import { logError, logRequest, logResponse, safeParseForLog } from "@/lib/apiLogger";
import { clearSession, readSessionSnapshot } from "../session";
import { ApiError } from "./errors";
import {
  decodeEmptyData, decodeGuardedData, decodeUnknownData, invalidResponse, isSuccessEnvelope
} from "./types";
import type { ApiDataGuard, ApiEnvelope, ResponseDecoder } from "./types";

interface RequestOptions {
  readonly method?: "GET" | "POST" | "PATCH" | "DELETE" | "PUT";
  readonly body?: unknown;
}
type CsrfToken = { readonly token: string; readonly headerName: string };
function isCsrfToken(value: unknown): value is CsrfToken {
  return typeof value === "object" && value !== null &&
    "token" in value && typeof value.token === "string" && value.token.length > 0 &&
    "headerName" in value && value.headerName === "X-CSRF-TOKEN";
}
function isErrorEnvelope(value: unknown): value is { readonly isSuccess: false; readonly statusCode: string; readonly message: string } {
  return typeof value === "object" && value !== null &&
    "isSuccess" in value && value.isSuccess === false &&
    "statusCode" in value && typeof value.statusCode === "string" &&
    "message" in value && typeof value.message === "string";
}
async function rawFetch<T>(path: string, options: RequestOptions, decode: ResponseDecoder<T>): Promise<T> {
  if (!path.startsWith("/bff/")) throw new ApiError("요청 경로가 올바르지 않아요.", 0, "INVALID_CLIENT_ROUTE");
  const sessionAtStart = readSessionSnapshot();
  const expireSession = () => { if (readSessionSnapshot() === sessionAtStart) clearSession(); };
  const { method = "GET", body } = options;
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (method !== "GET" || path.startsWith("/bff/auth/naver/url")) {
    const csrf = await rawFetch("/bff/auth/csrf", { method: "GET" },
      (value, status) => decodeGuardedData(value, status, isCsrfToken));
    headers[csrf.data.headerName] = csrf.data.token;
  }
  logRequest({ method, url: path, headers, body });
  const start = performance.now();
  let response: Response;
  try {
    response = await fetch(path, {
      method, headers, credentials: "include", cache: "no-store",
      signal: AbortSignal.timeout(10_000),
      body: body === undefined ? undefined : JSON.stringify(body)
    });
  } catch (error) {
    logError({ method, url: path, durationMs: Math.round(performance.now() - start), error });
    throw new ApiError("네트워크 요청에 실패했어요.", 0, "NETWORK_ERROR", error);
  }
  const rawText = await response.text();
  logResponse({ method, url: path, status: response.status, statusText: response.statusText,
    durationMs: Math.round(performance.now() - start), headers: response.headers, body: safeParseForLog(rawText) });
  let value: unknown;
  try { value = JSON.parse(rawText); } catch (error) {
    if (!(error instanceof SyntaxError)) throw error;
    if (response.status === 401) expireSession();
    throw new ApiError(rawText.length ? "서버 응답을 처리할 수 없어요." : "서버 응답이 비어 있어요.",
      response.status, rawText.length ? "MALFORMED_RESPONSE" : "EMPTY_RESPONSE");
  }
  if (isErrorEnvelope(value)) {
    if (response.status === 401 || ["AUT_001", "AUT_006", "AUT_007", "AUT_008"].includes(value.statusCode)) expireSession();
    throw new ApiError(value.message, response.status, value.statusCode, value);
  }
  if (response.status === 401) {
    expireSession();
    throw new ApiError("다시 로그인해 주세요.", 401, "UNAUTHORIZED");
  }
  if (!response.ok) throw new ApiError(`요청에 실패했어요. (${response.status})`, response.status, "HTTP_ERROR", value);
  if (!isSuccessEnvelope(value)) throw invalidResponse(value, response.status);
  return decode(value, response.status);
}
function dataDecoder<T>(guard?: ApiDataGuard<T>): ResponseDecoder<ApiEnvelope<T> | ApiEnvelope<unknown>> {
  if (guard === undefined) return decodeUnknownData;
  return (value, status) => decodeGuardedData(value, status, guard);
}
function get<T>(path: string, guard: ApiDataGuard<T>): Promise<ApiEnvelope<T>>;
function get<T extends ApiEnvelope<unknown>>(path: string): Promise<T>;
function get<T>(path: string, guard?: ApiDataGuard<T>): Promise<ApiEnvelope<T> | ApiEnvelope<unknown>> {
  return rawFetch(path, { method: "GET" }, dataDecoder(guard));
}
function post<T>(path: string, body: unknown, guard: ApiDataGuard<T>): Promise<ApiEnvelope<T>>;
function post<T extends ApiEnvelope<unknown>>(path: string, body?: unknown): Promise<T>;
function post<T>(path: string, body?: unknown, guard?: ApiDataGuard<T>): Promise<ApiEnvelope<T> | ApiEnvelope<unknown>> {
  return rawFetch(path, { method: "POST", body }, dataDecoder(guard));
}
function put<T>(path: string, body: unknown, guard: ApiDataGuard<T>): Promise<ApiEnvelope<T>>;
function put<T extends ApiEnvelope<unknown>>(path: string, body?: unknown): Promise<T>;
function put<T>(path: string, body?: unknown, guard?: ApiDataGuard<T>): Promise<ApiEnvelope<T> | ApiEnvelope<unknown>> {
  return rawFetch(path, { method: "PUT", body }, dataDecoder(guard));
}
function patch<T>(path: string, body: unknown, guard: ApiDataGuard<T>): Promise<ApiEnvelope<T>>;
function patch<T extends ApiEnvelope<unknown>>(path: string, body?: unknown): Promise<T>;
function patch<T>(path: string, body?: unknown, guard?: ApiDataGuard<T>): Promise<ApiEnvelope<T> | ApiEnvelope<unknown>> {
  return rawFetch(path, { method: "PATCH", body }, dataDecoder(guard));
}
function remove<T>(path: string, body: unknown, guard: ApiDataGuard<T>): Promise<ApiEnvelope<T>>;
function remove<T extends ApiEnvelope<unknown>>(path: string, body?: unknown): Promise<T>;
function remove<T>(path: string, body?: unknown, guard?: ApiDataGuard<T>): Promise<ApiEnvelope<T> | ApiEnvelope<unknown>> {
  return rawFetch(path, { method: "DELETE", body }, dataDecoder(guard));
}
export const apiClient = {
  get, post, put, patch, delete: remove,
  postEmpty: (path: string, body?: unknown) => rawFetch(path, { method: "POST", body }, decodeEmptyData),
  putEmpty: (path: string, body?: unknown) => rawFetch(path, { method: "PUT", body }, decodeEmptyData),
  deleteEmpty: (path: string, body?: unknown) => rawFetch(path, { method: "DELETE", body }, decodeEmptyData)
};
