import type { NextRequest } from "next/server";
import { forwardBackendRequest } from "@/lib/server/backendGateway";

export function DELETE(request: NextRequest): Promise<Response> {
  return forwardBackendRequest(request, "/api/v1/auth/logout", {
    method: "DELETE",
  });
}
