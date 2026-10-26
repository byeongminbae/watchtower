import ky from "ky";
import { headers } from "next/headers";
import { redirect } from "next/navigation";
import { isSessionPrincipal, type SessionPrincipal } from "@/lib/session";
import { ApiError } from "@/lib/api/errors";
import { sanitizeReturnPath } from "@/lib/oauthTransaction";

type SessionResult =
  | { readonly kind: "authenticated"; readonly principal: SessionPrincipal }
  | { readonly kind: "anonymous" }
  | { readonly kind: "unavailable" };

export async function readBackendSession(cookie: string): Promise<SessionResult> {
  const base = process.env.INTERNAL_API_BASE_URL;
  if (base === undefined || !URL.canParse(base)) return { kind: "unavailable" };
  const url = new URL("/api/v1/auth/session", base);
  if (!["http:", "https:"].includes(url.protocol) || url.username || url.password) return { kind: "unavailable" };
  try {
    const response = await ky(url, { headers: { cookie }, cache: "no-store", retry: 0,
      timeout: 10_000, redirect: "manual", throwHttpErrors: false });
    const value: unknown = await response.json();
    if (response.ok && typeof value === "object" && value !== null &&
      "isSuccess" in value && value.isSuccess === true && "data" in value && isSessionPrincipal(value.data)) {
      return { kind: "authenticated", principal: value.data };
    }
    if (response.status === 401 || (typeof value === "object" && value !== null &&
      "isSuccess" in value && value.isSuccess === false && "statusCode" in value &&
      typeof value.statusCode === "string" && ["AUT_001", "AUT_006", "AUT_007", "AUT_008"].includes(value.statusCode))) {
      return { kind: "anonymous" };
    }
    return { kind: "unavailable" };
  } catch (error) {
    if (!(error instanceof Error)) throw error;
    return { kind: "unavailable" };
  }
}

export async function requireServerSession(adminOnly = false): Promise<SessionPrincipal> {
  const requestHeaders = await headers();
  const result = await readBackendSession(requestHeaders.get("cookie") ?? "");
  if (result.kind === "anonymous") {
    const path = sanitizeReturnPath(requestHeaders.get("x-watchtower-return-path") ?? "/watches");
    redirect(`/login?next=${encodeURIComponent(path)}`);
  }
  if (result.kind === "unavailable") throw new ApiError("로그인 상태를 확인할 수 없어요. 다시 시도해 주세요.", 503);
  if (adminOnly && result.principal.role !== "ADMIN") redirect("/watches");
  return result.principal;
}
