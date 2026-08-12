import { InvalidTokenError, jwtDecode } from "jwt-decode";
import type { MemberRole } from "@/types/domain";

export type WatchtowerJwtPayload = {
  readonly sub: string;
  readonly role: MemberRole;
  readonly exp: number;
  readonly iat: number;
};

export type WatchtowerRefreshJwtPayload = {
  readonly sub: string;
  readonly exp: number;
  readonly iat: number;
};

function decodePayload(token: string): unknown | null {
  try {
    return jwtDecode<unknown>(token);
  } catch (error) {
    if (error instanceof InvalidTokenError) return null;
    throw error;
  }
}

function hasValidCommonClaims(
  value: unknown,
): value is { readonly sub: string; readonly exp: number; readonly iat: number } {
  if (
    typeof value !== "object" ||
    value === null ||
    !("sub" in value) ||
    !("exp" in value) ||
    !("iat" in value)
  ) {
    return false;
  }

  return (
    typeof value.sub === "string" &&
    /^[1-9]\d*$/.test(value.sub) &&
    Number.isSafeInteger(Number(value.sub)) &&
    typeof value.exp === "number" &&
    Number.isFinite(value.exp) &&
    typeof value.iat === "number" &&
    Number.isFinite(value.iat)
  );
}

export function decodeAccessToken(token: string): WatchtowerJwtPayload | null {
  const payload = decodePayload(token);
  if (!hasValidCommonClaims(payload) || !("role" in payload)) return null;
  if (payload.role !== "NORMAL" && payload.role !== "ADMIN") return null;

  return {
    sub: payload.sub,
    role: payload.role,
    exp: payload.exp,
    iat: payload.iat,
  };
}

export function decodeRefreshToken(token: string): WatchtowerRefreshJwtPayload | null {
  const payload = decodePayload(token);
  if (!hasValidCommonClaims(payload)) return null;

  return { sub: payload.sub, exp: payload.exp, iat: payload.iat };
}

export function isTokenExpired(
  payload: Pick<WatchtowerJwtPayload, "exp">,
  nowMilliseconds = Date.now(),
): boolean {
  return payload.exp * 1_000 <= nowMilliseconds;
}
