import { decodeAccessToken, decodeRefreshToken, isTokenExpired } from "@/lib/jwt";
import type { MemberRole } from "@/types/domain";

const ACCESS_COOKIE_NAME = "watchtower_jwt";
const REFRESH_COOKIE_NAME = "watchtower_refresh";
const COOKIE_BASE_ATTRIBUTES = "Path=/; SameSite=Lax";
const SESSION_CHANGED_EVENT = "watchtower:session-changed";
export const INVALID_SESSION_SNAPSHOT = "invalid";

export type SessionPrincipal = {
  readonly memberId: number;
  readonly role: MemberRole;
};

type ParsedSession = {
  readonly principal: SessionPrincipal;
  readonly accessExpiresAt: number;
  readonly refreshExpiresAt: number;
};

type ValidatedSession = {
  readonly accessToken: string;
  readonly session: ParsedSession;
};

function secureAttribute(): string {
  return typeof location !== "undefined" && location.protocol === "https:" ? "; Secure" : "";
}

function writeCookie(name: string, token: string, maxAge: number): void {
  document.cookie = `${name}=${encodeURIComponent(token)}; Max-Age=${maxAge}; ${COOKIE_BASE_ATTRIBUTES}${secureAttribute()}`;
}

function deleteCookie(name: string): void {
  document.cookie = `${name}=; Max-Age=0; ${COOKIE_BASE_ATTRIBUTES}${secureAttribute()}`;
}

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const prefix = `${name}=`;
  const cookie = document.cookie.split("; ").find((row) => row.startsWith(prefix));
  if (cookie === undefined) return null;

  try {
    return decodeURIComponent(cookie.slice(prefix.length));
  } catch (error) {
    if (error instanceof URIError) return null;
    throw error;
  }
}

function parseSession(accessToken: string, refreshToken: string, now: number): ParsedSession | null {
  const access = decodeAccessToken(accessToken);
  const refresh = decodeRefreshToken(refreshToken);
  if (
    access === null ||
    refresh === null ||
    access.sub !== refresh.sub ||
    isTokenExpired(access, now) ||
    isTokenExpired(refresh, now)
  ) {
    return null;
  }

  return {
    principal: { memberId: Number(access.sub), role: access.role },
    accessExpiresAt: access.exp,
    refreshExpiresAt: refresh.exp,
  };
}

function readStoredSession(): ValidatedSession | null {
  const accessToken = readCookie(ACCESS_COOKIE_NAME);
  const refreshToken = readCookie(REFRESH_COOKIE_NAME);
  if (accessToken === null || refreshToken === null) return null;

  const session = parseSession(accessToken, refreshToken, Date.now());
  if (session === null) return null;
  return { accessToken, session };
}

function readValidatedSession(): ValidatedSession | null {
  const validated = readStoredSession();
  if (validated === null) clearSession();
  return validated;
}

function emitSessionChanged(): void {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new window.Event(SESSION_CHANGED_EVENT));
  }
}

export function commitSession(accessToken: string, refreshToken: string): boolean {
  if (typeof document === "undefined") return false;
  const now = Date.now();
  const session = parseSession(accessToken, refreshToken, now);
  if (session === null) {
    clearSession();
    return false;
  }

  writeCookie(
    ACCESS_COOKIE_NAME,
    accessToken,
    Math.ceil(session.accessExpiresAt - now / 1_000),
  );
  writeCookie(
    REFRESH_COOKIE_NAME,
    refreshToken,
    Math.ceil(session.refreshExpiresAt - now / 1_000),
  );

  if (
    readCookie(ACCESS_COOKIE_NAME) !== accessToken ||
    readCookie(REFRESH_COOKIE_NAME) !== refreshToken
  ) {
    clearSession();
    return false;
  }

  emitSessionChanged();
  return true;
}

export function readSession(): SessionPrincipal | null {
  return readValidatedSession()?.session.principal ?? null;
}

export function readAccessToken(): string | null {
  return readValidatedSession()?.accessToken ?? null;
}

export function readSessionSnapshot(): string {
  const principal = readStoredSession()?.session.principal;
  if (principal !== undefined) return `${principal.memberId}:${principal.role}`;
  return readCookie(ACCESS_COOKIE_NAME) === null &&
    readCookie(REFRESH_COOKIE_NAME) === null
    ? ""
    : INVALID_SESSION_SNAPSHOT;
}

export function subscribeToSessionChanges(listener: () => void): () => void {
  if (typeof window === "undefined") return () => undefined;
  window.addEventListener(SESSION_CHANGED_EVENT, listener);
  return () => window.removeEventListener(SESSION_CHANGED_EVENT, listener);
}

export function clearSession(): void {
  if (typeof document === "undefined") return;
  deleteCookie(ACCESS_COOKIE_NAME);
  deleteCookie(REFRESH_COOKIE_NAME);
  emitSessionChanged();
}
