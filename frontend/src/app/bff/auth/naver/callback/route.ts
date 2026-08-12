import type { NextRequest } from "next/server";
import {
  forwardBackendRequest,
  selectedSearchParams,
} from "@/lib/server/backendGateway";

export function GET(request: NextRequest): Promise<Response> {
  return forwardBackendRequest(request, "/api/v1/auth/naver/callback", {
    method: "GET",
    searchParams: selectedSearchParams(request, ["code", "state"]),
  });
}
