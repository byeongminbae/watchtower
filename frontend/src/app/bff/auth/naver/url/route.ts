import type { NextRequest } from "next/server";
import {
  forwardBackendRequest,
  selectedSearchParams,
} from "@/lib/server/backendGateway";

export function GET(request: NextRequest): Promise<Response> {
  return forwardBackendRequest(request, "/api/v1/auth/naver/url", {
    method: "GET",
    searchParams: selectedSearchParams(request, ["state"]),
  });
}
