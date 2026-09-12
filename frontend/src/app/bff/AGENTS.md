<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# BFF route handlers

# Overview
- Next.js Route Handlers that proxy the browser to the actual Spring Boot backend: **browser → `/bff/*` route handler (server-side) → backend `/api/v1/*`**. Introduced so the browser never calls the backend origin directly, moving cookie/JWT forwarding and CORS/origin concerns server-side (commit "브라우저가 백엔드 API 경로를 직접 호출하지 않도록 Next BFF로 전환").
- Every handler is a thin wrapper around `forwardBackendRequest` from `src/lib/server/backendGateway.ts` (the shared proxy helper — see `src/lib/AGENTS.md`).
- `src/lib/api/client.ts`'s `apiClient` refuses to call anything outside `/bff/*` (`rawFetch` throws `INVALID_CLIENT_ROUTE` otherwise), so this directory is the *only* legal backend entry point from client code.

# Where to look
- `auth/logout/route.ts`: `DELETE` → backend `DELETE /api/v1/auth/logout`.
- `auth/naver/url/route.ts`: `GET` → backend `GET /api/v1/auth/naver/url`, forwarding only the `state` query param (`selectedSearchParams`).
- `auth/naver/callback/route.ts`: `GET` → backend `GET /api/v1/auth/naver/callback`, forwarding only `code` and `state`.
- `auth/renew/route.ts`: `POST` — validates the JSON body has a non-empty `refreshToken` string, then forwards to backend `POST /api/v1/auth/renew` with `refreshToken` moved into the **query string** (not the body).
- `member/[memberId]/route.ts`: `GET` — validates `memberId` matches `/^[1-9]\d*$/`, then forwards to backend `GET /api/v1/member/{memberId}`.
- `backend-routes.test.ts`: vitest suite (Given/When/Then) asserting exact backend URLs, header passthrough, the `503 FRONTEND_BACKEND_UNAVAILABLE` fail-closed path when `INTERNAL_API_BASE_URL` is unset, and the `502 FRONTEND_BACKEND_REQUEST_FAILED` path on timeout.

# Local conventions
- Handler pattern: optionally validate input (return `invalidBffRequest(message)` → 400 on failure), then call `forwardBackendRequest(request, backendPathname, { method, searchParams })` and return its `Response` directly.
- Handlers are typed `(request: NextRequest) => Promise<Response>` (or with a route `context` for dynamic segments); no handler touches `fetch` directly.

# Local anti-patterns / gotchas
- `INTERNAL_API_BASE_URL` (server-only, read in `backendGateway.ts`) is a *different* variable from the frontend-wide `NEXT_PUBLIC_API_BASE_URL` noted in the root `AGENTS.md` — don't conflate them when debugging connectivity.
- `forwardedHeaders` in `backendGateway.ts` only relays `accept`, `authorization`, and `cookie`; any other header added to a browser request (a trace id, a custom flag) silently never reaches the backend.
- `auth/renew`'s BFF route receives `refreshToken` in a JSON body but forwards it to the backend as a query param — a gotcha to remember when adding similar proxy routes.
- `resolveBackendUrl` fails closed (503) if `INTERNAL_API_BASE_URL` is missing, unparsable, non-http(s), or contains embedded credentials — it never throws, so a malformed env var in local dev shows up only as a 503 on every route, not a startup error.
