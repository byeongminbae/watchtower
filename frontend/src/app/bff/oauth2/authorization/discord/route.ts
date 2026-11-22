import type { NextRequest } from "next/server";
import { forwardBackendRedirect } from "@/lib/server/backendGateway";

export function GET(request: NextRequest): Promise<Response> {
  return forwardBackendRedirect(request, "/api/v1/oauth2/authorization/discord", new URLSearchParams());
}
