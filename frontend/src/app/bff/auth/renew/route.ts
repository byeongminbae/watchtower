import type { NextRequest } from "next/server";
import {
  forwardBackendRequest,
  invalidBffRequest,
} from "@/lib/server/backendGateway";

type RenewRequest = {
  readonly refreshToken: string;
};

function isRenewRequest(value: unknown): value is RenewRequest {
  return (
    typeof value === "object" &&
    value !== null &&
    !Array.isArray(value) &&
    "refreshToken" in value &&
    typeof value.refreshToken === "string" &&
    value.refreshToken.length > 0
  );
}

export async function POST(request: NextRequest): Promise<Response> {
  let body: unknown;
  try {
    body = await request.json();
  } catch (error) {
    if (error instanceof SyntaxError) {
      return invalidBffRequest("세션 갱신 요청 형식이 올바르지 않습니다.");
    }
    throw error;
  }

  if (!isRenewRequest(body)) {
    return invalidBffRequest("세션 갱신 요청 형식이 올바르지 않습니다.");
  }

  return forwardBackendRequest(request, "/api/v1/auth/renew", {
    method: "POST",
    searchParams: new URLSearchParams({ refreshToken: body.refreshToken }),
  });
}
