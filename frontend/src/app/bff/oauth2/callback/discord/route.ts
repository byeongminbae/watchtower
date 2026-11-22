import type { NextRequest } from "next/server";
import { forwardBackendRedirect, selectedSearchParams } from "@/lib/server/backendGateway";

export function GET(request: NextRequest): Promise<Response> {
  return forwardBackendRedirect(
    request,
    "/api/v1/oauth2/callback/discord",
    selectedSearchParams(request, ["code", "state", "error", "error_description"]),
  );
}
