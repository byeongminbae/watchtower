// 백엔드가 네이버 로그인 콜백 처리 후 심어주는 JWT 쿠키를 매번 직접 읽는다.
// httpOnly가 아니므로 document.cookie로 접근 가능하다고 가정한다.
// 이 쿠키가 유일한 인증 상태의 출처(source of truth)이며, 별도 localStorage 캐시는 두지 않는다.
// (백엔드가 로그아웃 시 쿠키를 지우면 프론트도 즉시 반영되어야 하므로)

// TODO: 실제 백엔드가 심는 쿠키 이름으로 맞춰야 함. 스펙에 명시되어 있지 않아 관례적인 이름으로 추론.
const JWT_COOKIE_NAME = "watchtower_jwt";

export function getJwtFromCookie(): string | null {
  if (typeof document === "undefined") return null;

  const match = document.cookie
    .split("; ")
    .find((row) => row.startsWith(`${JWT_COOKIE_NAME}=`));

  if (!match) return null;
  return decodeURIComponent(match.substring(JWT_COOKIE_NAME.length + 1));
}
