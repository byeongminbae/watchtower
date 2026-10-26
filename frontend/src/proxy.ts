import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";
import { readBackendSession } from "@/lib/server/session";

export async function proxy(request: NextRequest): Promise<NextResponse> {
  if (request.nextUrl.pathname !== "/login") {
    const headers = new Headers(request.headers);
    headers.set("x-watchtower-return-path", request.nextUrl.pathname + request.nextUrl.search);
    return NextResponse.next({ request: { headers } });
  }
  if (!request.cookies.has("JSESSIONID")) return NextResponse.next();
  const result = await readBackendSession(request.headers.get("cookie") ?? "");
  if (result.kind !== "authenticated") return NextResponse.next();
  return NextResponse.redirect(new URL(result.principal.role === "ADMIN" ? "/admin" : "/watches", request.url));
}
export const config = { matcher: ["/login", "/watches/:path*", "/mypage/:path*", "/admin/:path*"] };
