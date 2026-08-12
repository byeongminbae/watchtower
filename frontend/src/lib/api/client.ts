import { logError, logRequest, logResponse, safeParseForLog } from "@/lib/apiLogger";
import { getJwtFromCookie } from "@/lib/jwtCookie";
import { clearSession } from "../session";
import { ApiError } from "./errors";
import {
  decodeEmptyData,
  decodeGuardedData,
  decodeUnknownData,
  invalidResponse,
  isSuccessEnvelope,
} from "./types";
import type {
  ApiDataGuard,
  ApiEnvelope,
  ResponseDecoder,
} from "./types";

interface RequestOptions {
  readonly method?: "GET" | "POST" | "PATCH" | "DELETE" | "PUT";
  readonly body?: unknown;
}

type ErrorEnvelope = {
  readonly success: false;
  readonly statusCode: string;
  readonly message: string;
  readonly timestamp: string;
};

type BodyParseResult =
  | { readonly kind: "empty" }
  | { readonly kind: "malformed"; readonly cause: SyntaxError }
  | { readonly kind: "json"; readonly value: unknown };

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isErrorEnvelope(value: unknown): value is ErrorEnvelope {
  return (
    isRecord(value) &&
    value.success === false &&
    typeof value.statusCode === "string" &&
    typeof value.message === "string" &&
    typeof value.timestamp === "string"
  );
}

function parseBody(rawText: string): BodyParseResult {
  if (rawText.length === 0) return { kind: "empty" };

  try {
    const value: unknown = JSON.parse(rawText);
    return { kind: "json", value };
  } catch (error) {
    if (error instanceof SyntaxError) return { kind: "malformed", cause: error };
    throw error;
  }
}

async function rawFetch<T>(
  path: string,
  options: RequestOptions,
  decode: ResponseDecoder<T>,
): Promise<T> {
  if (!path.startsWith("/bff/")) {
    throw new ApiError(
      "프론트엔드 API 경로가 올바르지 않습니다.",
      0,
      "INVALID_CLIENT_ROUTE",
      path,
    );
  }

  const { method = "GET", body } = options;
  const url = path;
  const jwt = getJwtFromCookie();
  const headers: Record<string, string> =
    jwt === null
      ? { "Content-Type": "application/json" }
      : { "Content-Type": "application/json", Authorization: `Bearer ${jwt}` };

  logRequest({ method, url, headers, body });
  const start = performance.now();

  let response: Response;
  try {
    response = await fetch(url, {
      method,
      headers,
      credentials: "include",
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (error) { // no-excuse-ok: catch — this transport boundary normalizes all fetch rejection values.
    const durationMs = Math.round(performance.now() - start);
    logError({ method, url, durationMs, error });
    throw new ApiError("네트워크 오류가 발생했습니다.", 0, "NETWORK_ERROR", error);
  }

  const durationMs = Math.round(performance.now() - start);
  const rawText = await response.text();
  const bodyResult = parseBody(rawText);
  const parsedForLog = safeParseForLog(rawText);

  logResponse({
    method,
    url,
    status: response.status,
    statusText: response.statusText,
    durationMs,
    headers: response.headers,
    body: parsedForLog,
  });

  if (response.status === 401) {
    clearSession();
    const value = bodyResult.kind === "json" ? bodyResult.value : parsedForLog;
    if (isErrorEnvelope(value)) {
      throw new ApiError(value.message, 401, value.statusCode, value);
    }
    throw new ApiError(
      "인증이 만료되었습니다. 다시 로그인해주세요.",
      401,
      "UNAUTHORIZED",
      value,
    );
  }

  switch (bodyResult.kind) {
    case "empty":
      throw new ApiError(
        "API 응답 본문이 비어 있습니다.",
        response.status,
        "EMPTY_RESPONSE",
      );
    case "malformed":
      throw new ApiError(
        "API 응답이 올바른 JSON 형식이 아닙니다.",
        response.status,
        "MALFORMED_RESPONSE",
        rawText,
      );
    case "json":
      if (isErrorEnvelope(bodyResult.value)) {
        throw new ApiError(
          bodyResult.value.message,
          response.status,
          bodyResult.value.statusCode,
          bodyResult.value,
        );
      }
      if (!isSuccessEnvelope(bodyResult.value)) {
        throw invalidResponse(bodyResult.value, response.status);
      }
      if (!response.ok) {
        throw new ApiError(
          `요청에 실패했습니다. (${response.status})`,
          response.status,
          "HTTP_ERROR",
          bodyResult.value,
        );
      }
      return decode(bodyResult.value, response.status);
    default: {
      const exhaustive: never = bodyResult;
      throw new ApiError(
        "처리할 수 없는 API 응답 상태입니다.",
        response.status,
        "INVALID_RESPONSE",
        exhaustive,
      );
    }
  }
}

function dataDecoder<T>(
  guard?: ApiDataGuard<T>,
): ResponseDecoder<ApiEnvelope<T> | ApiEnvelope<unknown>> {
  if (guard === undefined) return decodeUnknownData;
  return (value, status) => decodeGuardedData(value, status, guard);
}

function get<T>(
  path: string,
  guard: ApiDataGuard<T>,
): Promise<ApiEnvelope<T>>;
function get<T extends ApiEnvelope<unknown>>(path: string): Promise<T>;
function get<T>(
  path: string,
  guard?: ApiDataGuard<T>,
): Promise<ApiEnvelope<T> | ApiEnvelope<unknown>> {
  return rawFetch(path, { method: "GET" }, dataDecoder(guard));
}

function post<T>(
  path: string,
  body: unknown,
  guard: ApiDataGuard<T>,
): Promise<ApiEnvelope<T>>;
function post<T extends ApiEnvelope<unknown>>(
  path: string,
  body?: unknown,
): Promise<T>;
function post<T>(
  path: string,
  body?: unknown,
  guard?: ApiDataGuard<T>,
): Promise<ApiEnvelope<T> | ApiEnvelope<unknown>> {
  return rawFetch(path, { method: "POST", body }, dataDecoder(guard));
}

function patch<T>(
  path: string,
  body: unknown,
  guard: ApiDataGuard<T>,
): Promise<ApiEnvelope<T>>;
function patch<T extends ApiEnvelope<unknown>>(
  path: string,
  body?: unknown,
): Promise<T>;
function patch<T>(
  path: string,
  body?: unknown,
  guard?: ApiDataGuard<T>,
): Promise<ApiEnvelope<T> | ApiEnvelope<unknown>> {
  return rawFetch(path, { method: "PATCH", body }, dataDecoder(guard));
}

function remove<T>(
  path: string,
  body: unknown,
  guard: ApiDataGuard<T>,
): Promise<ApiEnvelope<T>>;
function remove<T extends ApiEnvelope<unknown>>(
  path: string,
  body?: unknown,
): Promise<T>;
function remove<T>(
  path: string,
  body?: unknown,
  guard?: ApiDataGuard<T>,
): Promise<ApiEnvelope<T> | ApiEnvelope<unknown>> {
  return rawFetch(path, { method: "DELETE", body }, dataDecoder(guard));
}

export const apiClient = {
  get,
  post,
  patch,
  delete: remove,
  deleteEmpty: (path: string, body?: unknown) =>
    rawFetch(path, { method: "DELETE", body }, decodeEmptyData),
};
