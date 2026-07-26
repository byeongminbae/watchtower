import { logError, logRequest, logResponse, safeParseForLog } from "@/lib/apiLogger";
import { getJwtFromCookie } from "@/lib/jwtCookie";
import { ApiError, ApiErrorBody } from "./errors";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "DELETE" | "PUT";
  body?: unknown;
  // 401을 받았을 때 /auth/renew로 자동 재시도할지 여부. 재발급 요청 자체에는 false로 넘겨 무한루프 방지.
  skipAuthRetry?: boolean;
}

let renewPromise: Promise<boolean> | null = null;

// 인증 만료(401) 시 /auth/renew를 호출해 쿠키(JWT)를 갱신한다.
// 응답 바디로 새 토큰을 받는 게 아니라, 백엔드가 Set-Cookie로 쿠키 자체를 갱신해준다고 가정한다.
// 동시에 여러 요청이 401을 맞아도 재발급은 한 번만 수행되도록 Promise를 공유한다.
async function renewSession(): Promise<boolean> {
  if (renewPromise) return renewPromise;

  renewPromise = (async () => {
    try {
      await rawFetch("/api/v1/auth/renew", { method: "POST", skipAuthRetry: true });
      // 갱신 후 쿠키가 실제로 새로 세팅되었는지 확인
      return getJwtFromCookie() !== null;
    } catch {
      return false;
    } finally {
      renewPromise = null;
    }
  })();

  return renewPromise;
}

async function rawFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, skipAuthRetry } = options;
  const url = `${BASE_URL}${path}`;
  // 매 요청마다 쿠키에서 직접 읽는다 (localStorage 등 별도 캐시를 두지 않음 - 쿠키가 유일한 source of truth).
  const jwt = getJwtFromCookie();

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (jwt) headers.Authorization = `Bearer ${jwt}`;

  logRequest({ method, url, headers, body });
  const start = performance.now();

  let response: Response;
  try {
    response = await fetch(url, {
      method,
      headers,
      credentials: "include", // 쿠키(JWT) 자동 첨부/갱신을 위해 필요
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (err) {
    const durationMs = Math.round(performance.now() - start);
    logError({ method, url, durationMs, error: err });
    throw new ApiError("네트워크 오류가 발생했습니다.", 0, "NETWORK_ERROR", err);
  }

  const durationMs = Math.round(performance.now() - start);
  const rawText = await response.text();
  const parsedBody = safeParseForLog(rawText);

  logResponse({
    method,
    url,
    status: response.status,
    statusText: response.statusText,
    durationMs,
    headers: response.headers,
    body: parsedBody,
  });

  // 인증 만료 시 한 번 재발급(쿠키 갱신) 시도 후 재요청
  if (response.status === 401 && !skipAuthRetry) {
    const renewed = await renewSession();
    if (renewed) {
      return rawFetch<T>(path, { ...options, skipAuthRetry: true });
    }
    throw new ApiError("인증이 만료되었습니다. 다시 로그인해주세요.", 401, "UNAUTHORIZED", parsedBody);
  }

  if (!response.ok) {
    const errBody = parsedBody as ApiErrorBody | undefined;
    throw new ApiError(
      errBody?.error?.message ?? `요청에 실패했습니다. (${response.status})`,
      response.status,
      errBody?.error?.code,
      parsedBody,
    );
  }

  return parsedBody as T;
}

export const apiClient = {
  get: <T>(path: string) => rawFetch<T>(path, { method: "GET" }),
  post: <T>(path: string, body?: unknown) => rawFetch<T>(path, { method: "POST", body }),
  patch: <T>(path: string, body?: unknown) => rawFetch<T>(path, { method: "PATCH", body }),
  delete: <T>(path: string, body?: unknown) => rawFetch<T>(path, { method: "DELETE", body }),
};
