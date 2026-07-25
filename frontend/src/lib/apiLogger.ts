// 브라우저 개발자 콘솔에 API 요청/응답의 raw 데이터를 그룹핑해서 출력한다.
// 프로덕션에서도 일단 켜둔다 (백엔드가 아직 개발 서버 단계이고, 스펙 검증에 유용하기 때문).
// 필요 시 NEXT_PUBLIC_API_LOG=false 로 끌 수 있게 해둔다.

const LOG_ENABLED = process.env.NEXT_PUBLIC_API_LOG !== "false";

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

export function logRequest({ method, url, headers, body }: RequestLogInput) {
  if (!LOG_ENABLED || typeof window === "undefined") return;

  console.groupCollapsed(
    `%c▶ API Request%c ${method} ${url}`,
    "color:#3B82C4;font-weight:700;",
    "color:inherit;font-weight:400;",
  );
  console.log("Request URL:", url);
  console.log("Method:", method);
  if (headers) console.log("Headers:", headers);
  if (body !== undefined) console.log("Body:", body);
  console.groupEnd();
}

export function logResponse({ method, url, status, statusText, durationMs, headers, body }: ResponseLogInput) {
  if (!LOG_ENABLED || typeof window === "undefined") return;

  const ok = status >= 200 && status < 300;
  const color = ok ? "#4ADE80" : "#F87171";

  console.groupCollapsed(
    `%c◀ API Response%c ${method} ${url} — ${status} ${statusText} (${durationMs}ms)`,
    `color:${color};font-weight:700;`,
    "color:inherit;font-weight:400;",
  );
  console.log("Status:", status, statusText);
  console.log("Duration:", `${durationMs}ms`);
  if (headers) {
    const headerObj: Record<string, string> = {};
    headers.forEach((v, k) => (headerObj[k] = v));
    console.log("Headers:", headerObj);
  }
  console.log("Body:", body);
  console.groupEnd();
}

export function logError({ method, url, durationMs, error }: ErrorLogInput) {
  if (!LOG_ENABLED || typeof window === "undefined") return;

  console.groupCollapsed(
    `%c✖ API Error%c ${method} ${url} (${durationMs}ms)`,
    "color:#F87171;font-weight:700;",
    "color:inherit;font-weight:400;",
  );
  console.error("Error:", error);
  console.groupEnd();
}

export { safeParseForLog };
