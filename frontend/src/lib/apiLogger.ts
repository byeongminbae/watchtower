// 브라우저 개발자 콘솔에 API 요청/응답의 민감정보를 마스킹해 그룹핑해서 출력한다.
// 프로덕션에서도 일단 켜둔다 (백엔드가 아직 개발 서버 단계이고, 스펙 검증에 유용하기 때문).
// 필요 시 NEXT_PUBLIC_API_LOG=false 로 끌 수 있게 해둔다.

const LOG_ENABLED = process.env.NEXT_PUBLIC_API_LOG !== "false";
const REDACTED = "[REDACTED]";
const SENSITIVE_KEYS = new Set([
  "authorization",
  "cookie",
  "setcookie",
  "accesstoken",
  "refreshtoken",
]);

interface RequestLogInput {
  method: string;
  url: string;
  headers?: HeadersInit;
  body?: unknown;
}

interface ResponseLogInput {
  method: string;
  url: string;
  status: number;
  statusText: string;
  durationMs: number;
  headers?: Headers;
  body?: unknown;
}

interface ErrorLogInput {
  method: string;
  url: string;
  durationMs: number;
  error: unknown;
}

function safeParseForLog(text: string): unknown {
  if (!text) return undefined;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function normalizeKey(key: string): string {
  return key.toLowerCase().replaceAll("-", "").replaceAll("_", "");
}

function isSensitiveQueryKey(key: string): boolean {
  const normalized = normalizeKey(key);
  return normalized === "code" || normalized === "state" || SENSITIVE_KEYS.has(normalized);
}

function redactFragment(hash: string): string {
  if (!hash.startsWith("#") || !hash.includes("=")) return hash;

  const params = new URLSearchParams(hash.slice(1));
  let redacted = false;
  for (const key of Array.from(params.keys())) {
    if (isSensitiveQueryKey(key)) {
      params.set(key, REDACTED);
      redacted = true;
    }
  }
  return redacted ? `#${params.toString()}` : hash;
}

function redactMalformedUrl(url: string): string {
  return url.replace(/([?&#])([^=&#]+)=([^&#]*)/giu, (match, prefix, key) =>
    isSensitiveQueryKey(key) ? `${prefix}${key}=${REDACTED}` : match,
  );
}

function redactStringValue(value: string): string {
  return value.startsWith("/") || /^https?:\/\//iu.test(value) ? redactUrl(value) : value;
}

function redactValue(value: unknown): unknown {
  if (typeof value === "string") return redactStringValue(value);
  if (Array.isArray(value)) return value.map((item) => redactValue(item));
  if (value instanceof Error) {
    return { name: value.name, message: redactStringValue(value.message) };
  }
  if (typeof value !== "object" || value === null) return value;

  const redacted: Record<string, unknown> = {};
  for (const [key, nestedValue] of Object.entries(value)) {
    redacted[key] = SENSITIVE_KEYS.has(normalizeKey(key))
      ? REDACTED
      : redactValue(nestedValue);
  }
  return redacted;
}

function redactHeaders(headers: HeadersInit): Record<string, string> {
  const redacted: Record<string, string> = {};
  if (headers instanceof Headers) {
    headers.forEach((value, key) => {
      redacted[key] = SENSITIVE_KEYS.has(normalizeKey(key)) ? REDACTED : value;
    });
    return redacted;
  }

  const entries = Array.isArray(headers) ? headers : Object.entries(headers);
  for (const [key, value] of entries) {
    redacted[key] = SENSITIVE_KEYS.has(normalizeKey(key)) ? REDACTED : value;
  }
  return redacted;
}

function redactUrl(url: string): string {
  const baseUrl = "https://watchtower.invalid";
  try {
    const parsed = new URL(url, baseUrl);
    for (const key of Array.from(parsed.searchParams.keys())) {
      if (isSensitiveQueryKey(key)) {
        parsed.searchParams.set(key, REDACTED);
      }
    }
    parsed.hash = redactFragment(parsed.hash);
    if (parsed.origin === baseUrl) {
      return `${parsed.pathname}${parsed.search}${parsed.hash}`;
    }
    return parsed.toString();
  } catch (error) {
    if (error instanceof TypeError) {
      return redactMalformedUrl(url);
    }
    throw error;
  }
}

function isOAuthCallbackUrl(url: string): boolean {
  return /\/auth\/naver\/callback(?:[/?#]|$)/u.test(url);
}

export function logRequest({ method, url, headers, body }: RequestLogInput): void {
  if (!LOG_ENABLED || typeof window === "undefined") return;
  const safeUrl = redactUrl(url);

  console.groupCollapsed(
    `%c▶ API Request%c ${method} ${safeUrl}`,
    "color:#3B82C4;font-weight:700;",
    "color:inherit;font-weight:400;",
  );
  console.log("Request URL:", safeUrl);
  console.log("Method:", method);
  if (headers) console.log("Headers:", redactHeaders(headers));
  if (body !== undefined) console.log("Body:", redactValue(body));
  console.groupEnd();
}

export function logResponse({ method, url, status, statusText, durationMs, headers, body }: ResponseLogInput): void {
  if (!LOG_ENABLED || typeof window === "undefined") return;
  const safeUrl = redactUrl(url);

  const ok = status >= 200 && status < 300;
  const color = ok ? "#4ADE80" : "#F87171";

  console.groupCollapsed(
    `%c◀ API Response%c ${method} ${safeUrl} — ${status} ${statusText} (${durationMs}ms)`,
    `color:${color};font-weight:700;`,
    "color:inherit;font-weight:400;",
  );
  console.log("Status:", status, statusText);
  console.log("Duration:", `${durationMs}ms`);
  if (headers) {
    console.log("Headers:", redactHeaders(headers));
  }
  console.log(
    "Body:",
    isOAuthCallbackUrl(url) ? REDACTED : redactValue(body),
  );
  console.groupEnd();
}

export function logError({ method, url, durationMs, error }: ErrorLogInput): void {
  if (!LOG_ENABLED || typeof window === "undefined") return;
  const safeUrl = redactUrl(url);

  console.groupCollapsed(
    `%c✖ API Error%c ${method} ${safeUrl} (${durationMs}ms)`,
    "color:#F87171;font-weight:700;",
    "color:inherit;font-weight:400;",
  );
  console.error("Error:", redactValue(error));
  console.groupEnd();
}

export { safeParseForLog };
