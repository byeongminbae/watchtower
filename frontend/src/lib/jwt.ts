import { jwtDecode } from "jwt-decode";
import { MemberRole } from "@/types/domain";

// 백엔드 JWT 클레임 구조는 스웨거에 명시되어 있지 않아, 일반적인 관례로 추론한다:
// { sub: memberId, role: "USER" | "ADMIN", exp, iat }
export interface WatchtowerJwtPayload {
  sub: string;
  role: MemberRole;
  exp: number;
  iat: number;
}

export function decodeAccessToken(token: string): WatchtowerJwtPayload | null {
  try {
    return jwtDecode<WatchtowerJwtPayload>(token);
  } catch {
    return null;
  }
}

export function isTokenExpired(payload: WatchtowerJwtPayload): boolean {
  return payload.exp * 1000 < Date.now();
}
