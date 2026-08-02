import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";
import {
  ACCESS_COOKIE_NAME,
  parseSessionPrincipal,
  REFRESH_COOKIE_NAME,
} from "@/lib/session";

export function proxy(request: NextRequest): NextResponse {
  if (request.nextUrl.pathname !== "/login") return NextResponse.next();

  const accessToken = request.cookies.get(ACCESS_COOKIE_NAME)?.value;
  const refreshToken = request.cookies.get(REFRESH_COOKIE_NAME)?.value;
  if (
    accessToken === undefined ||
    refreshToken === undefined ||
    parseSessionPrincipal(accessToken, refreshToken) === null
  ) {
    return NextResponse.next();
  }

  return NextResponse.redirect(new URL("/", request.url));
}

export const config = {
  matcher: "/login/:path*",
};
