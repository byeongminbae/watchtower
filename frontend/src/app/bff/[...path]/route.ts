import type { NextRequest } from "next/server";
import { bffError, forwardBackendRequest } from "@/lib/server/backendGateway";

const routes: readonly [RegExp, readonly string[]][] = [
  [/^auth\/(session|csrf)$/, ["GET"]],
  [/^member\/[1-9]\d*\/towers$/, ["GET"]],
  [/^towers$/, ["POST"]],
  [/^towers\/trending$/, ["GET"]],
  [/^towers\/[1-9]\d*$/, ["GET", "PUT", "DELETE"]],
  [/^towers\/[1-9]\d*\/conditions$/, ["GET", "POST"]],
  [/^towers\/[1-9]\d*\/conditions\/[1-9]\d*$/, ["PUT", "DELETE"]],
  [/^towers\/[1-9]\d*\/snapshots$/, ["GET"]],
  [/^notification\/channels$/, ["GET", "POST"]],
  [/^notification\/channels\/[1-9]\d*$/, ["GET", "PUT", "DELETE"]],
  [/^admin\/(towers|members)$/, ["GET"]],
  [/^admin\/towers\/[1-9]\d*$/, ["PUT"]],
  [/^admin\/members\/[1-9]\d*\/(roles|status)$/, ["PUT"]],
  [/^admin\/stats\/(towers|members|auth)$/, ["GET"]]
];
type Context = { readonly params: Promise<{ readonly path: string[] }> };
async function forward(request: NextRequest, context: Context): Promise<Response> {
  const pathname = (await context.params).path.join("/");
  const route = routes.find(([pattern]) => pattern.test(pathname));
  if (route === undefined) return bffError(404, "BFF_ROUTE_NOT_FOUND", "지원하지 않는 요청이에요.");
  if (!route[1].includes(request.method)) return bffError(405, "BFF_METHOD_NOT_ALLOWED", "허용하지 않는 요청 방식이에요.");
  return forwardBackendRequest(request, `/api/v1/${pathname}`);
}
export { forward as GET, forward as POST, forward as PUT, forward as DELETE };
