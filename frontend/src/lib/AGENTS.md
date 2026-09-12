<!-- Parent: ../../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# Data and session layer

# Overview
- One level deeper than `frontend/AGENTS.md`'s summary: the `api/` domain modules, the server-side `backendGateway.ts` proxy helper, and the auth/session file group (`session.ts`, `jwtCookie.ts`, `jwt.ts`, `oauthTransaction.ts`, `AuthContext.tsx`).

# Where to look
- `api/client.ts`: `apiClient` — the transport boundary. `rawFetch` requires every path to start with `/bff/` (throws `INVALID_CLIENT_ROUTE` otherwise), attaches `Authorization: Bearer <jwt>` from `jwtCookie.getJwtFromCookie()`, sends `credentials: "include"`, and on a `401` calls `clearSession()` before throwing — there is no automatic renew-and-retry here.
- `api/errors.ts`: `ApiError` (status/code/raw) and `BackendFeatureUnavailableError` (501, `BACKEND_FEATURE_UNAVAILABLE`).
- `api/availability.ts`: `backendFeatureInventory` — a `"domain.method"` → `contractClass` map (`callable` | `placeholder-data` | `empty-success` | `absent`). `rejectUnavailable(feature)` rejects with `BackendFeatureUnavailableError`; the type system only allows passing features *not* marked `callable`. This is the mechanism behind most `api/*.ts` methods being non-functional stubs.
- `api/types.ts`: `ApiEnvelope<T>`/`ApiEmptyEnvelope` shapes, `isSuccessEnvelope`, `decodeUnknownData`/`decodeGuardedData`/`decodeEmptyData`, `InvalidApiDataError`.
- `api/auth.ts`: `authApi` — `getNaverLoginUrl`, `loginWithNaverCallback`, `renewSession`, `logout` call real `/bff/auth/*` routes; `revokeNaverToken` is still a stub.
- `api/member.ts`: `memberApi.getMember` calls `/bff/member/{id}`; every other method (`updateMember`, `deleteMember`, `getMemberWatches`, `getMemberPaymentHistories`, `getMemberSubscription`, `cancelMemberSubscription`) is a stub.
- `api/watch.ts`, `api/payment.ts`, `api/admin.ts`: entirely stubs — no method in these three files is wired to a backend call yet.
- `api/index.ts`: barrel re-export of `client`, `errors`, `types`, `auth`, `member`, `watch`, `payment`, `admin`.
- `server/backendGateway.ts`: server-only proxy helper (`forwardBackendRequest`, `selectedSearchParams`, `invalidBffRequest`) used exclusively by `src/app/bff/*/route.ts` handlers — the counterpart to `api/client.ts` on the other side of the `/bff/*` boundary. Full detail in `src/app/bff/AGENTS.md`.
- `apiLogger.ts`: console-group request/response/error logger, on by default (even in production) unless `NEXT_PUBLIC_API_LOG=false`; redacts sensitive header/query/body keys and always fully redacts OAuth-callback response bodies regardless of shape (they may carry raw JWTs).
- `AuthContext.tsx`: subscribes to `session.ts` via `React.useSyncExternalStore` (`subscribeToSessionChanges`/`readSessionSnapshot`) rather than owning session state directly, so cross-tab cookie changes propagate on the next sync. `memberStatus` is a small state machine (`idle` → `loading` → `ready`/`unavailable`/`error`); a `BackendFeatureUnavailableError` or `InvalidApiDataError` from `memberApi.getMember` degrades to `unavailable`, not a hard error.
- `jwtCookie.ts`: one-line wrapper — `getJwtFromCookie()` just calls `session.readAccessToken()`; exists so `api/client.ts` doesn't import `session.ts` directly.
- `jwt.ts`: `decodeAccessToken`/`decodeRefreshToken`/`isTokenExpired` — `jwt-decode` wrappers with runtime shape validation (`sub` must be a positive-integer string; `decodeAccessToken` additionally requires `role` to be `"NORMAL"`/`"ADMIN"`). No signature verification.
- `oauthTransaction.ts`: sessionStorage-based CSRF/state handling for Naver OAuth. `createOAuthTransaction(returnPath)` stores a nonce + sanitized return path under `watchtower_oauth_transaction` with a 10-minute TTL; `consumeOAuthTransaction(state)` reads-and-deletes it, checking nonce match and TTL. `sanitizeReturnPath` blocks open-redirects (must start with `/`, no `//`, no backslash, no control chars, safe after repeated URI-decoding), falling back to `/watches`.
- `session.ts`: cookie-backed session store (`watchtower_jwt`/`watchtower_refresh`). `commitSession` writes both cookies then reads them back to confirm the write actually took (rolls back via `clearSession` if not) before emitting the `watchtower:session-changed` event. `subscribeToSessionChanges` also listens for `pageshow` to catch bfcache restores.
- `useApiData.ts`: fetch lifecycle hook (already summarized at a high level in `frontend/AGENTS.md`) — `loading` is derived by comparing a request-generation ref against the last-resolved generation, not a separate boolean set inside the effect, specifically to avoid a cascading render.

# Local conventions
- Auth data flow: `oauthTransaction` (state/returnPath) → `AuthContext.acceptSession` → `session.commitSession` (writes cookies) → `jwtCookie`/`session.readAccessToken` (read by `api/client.ts` for the `Authorization` header) → `app/bff/*` route handlers forward cookie + `Authorization` to the backend via `backendGateway.ts`.
- Before trusting any `api/*.ts` method works, check its `contractClass` in `api/availability.ts` — the file layout looks like a complete typed API client, but only five methods across `auth.ts`/`member.ts` are actually wired.

# Local anti-patterns / gotchas
- `session.readAccessToken()` calls `clearSession()` as a side effect whenever the stored session is invalid or expired — simply reading the access token can silently log the user out.
- `jwt.ts` performs no signature verification; treat decoded claims as shape-checked, not as a security boundary, on the client.
