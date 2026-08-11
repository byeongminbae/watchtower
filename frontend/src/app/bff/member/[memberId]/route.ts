import type { NextRequest } from "next/server";
import {
  forwardBackendRequest,
  invalidBffRequest,
} from "@/lib/server/backendGateway";

type MemberRouteContext = {
  readonly params: Promise<{ readonly memberId: string }>;
};

export async function GET(
  request: NextRequest,
  context: MemberRouteContext,
): Promise<Response> {
  const { memberId } = await context.params;
  if (!/^[1-9]\d*$/u.test(memberId)) {
    return invalidBffRequest("회원 식별자가 올바르지 않습니다.");
  }

  return forwardBackendRequest(request, `/api/v1/member/${memberId}`, {
    method: "GET",
  });
}
