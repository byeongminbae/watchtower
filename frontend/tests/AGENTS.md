<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# Playwright e2e suite

# Overview
- Playwright config lives at `frontend/playwright.config.ts`: `testDir: "./tests"`, `testMatch: "**/*.spec.ts"` (only `.spec.ts` runs here — co-located `*.test.ts(x)` unit tests elsewhere in the tree are vitest and excluded), `webServer` runs `npm run build && npm run start` against `http://127.0.0.1:3000` (a production build, not `next dev`), and `storageState` is hard-set to empty cookies/origins so every test starts logged out.

# Where to look
- `e2e/login-redirect.spec.ts`: `/login` redirects to `/` when valid session cookies already exist — checked both pre-hydration (`javaScriptEnabled: false`, server render) and via a back/forward-cache (bfcache) restore.
- `e2e/mypage-profile.spec.ts`: full mypage flow — profile display, nickname edit (cancel/save), session renew (`/bff/auth/renew` mock), logout (`/bff/auth/logout` mock, asserts cookies are cleared), screenshots across mobile/tablet/desktop.
- `e2e/oauth-callback.spec.ts`: the largest spec — full Naver OAuth happy path (state issuance → provider redirect → callback exchange → session survives reload/protected nav), provider rejection, missing/mismatched/replayed `state`, an unsafe stored return path (open-redirect attempt), and callback failure modes (error envelope, malformed token shape, invalid JWT pair, network failure) at mobile viewport.
- `e2e/public-api-boundary.spec.ts`: asserts the browser only ever requests `/bff/*` (never `/api/v1/*`) during the OAuth callback — the most direct test of the BFF architecture documented in `src/app/bff/AGENTS.md`.
- `e2e/session-unauthorized.spec.ts`: an in-flight request returning `401` triggers immediate client-side logout (header + guard state) with no page reload.
- `e2e/site-visual-qa.spec.ts`: crawls every public/member/admin route at 3 viewports, asserts zero horizontal overflow, and screenshots into `test-results/visual-qa/current/`; also covers focus/press/reduced-motion interaction states on the homepage CTA.
- `fixtures/index.ts`: extends the base Playwright `test` with an `oauth` fixture — `storeTransaction(page, state, returnPath?)` seeds `sessionStorage`'s `watchtower_oauth_transaction`; `validTokens(memberId?)` mints unsigned (`alg: "none"`) fixture JWTs matching `WatchtowerJwtPayload`/`WatchtowerRefreshJwtPayload`; `callbackRequestCount()` counts `/bff/auth/naver/callback` requests via `page.on("request")`. All e2e specs import `{ expect, test }` from here, not `@playwright/test` directly.
- `fixtures/infrastructure.spec.ts`: minimal smoke spec confirming the dev-server/Playwright wiring itself works.
- `fixtures/vitest-environment.test.tsx`: **not** a Playwright spec despite its folder — a vitest + `@testing-library/react` smoke test verifying `jest-dom` matchers are available; `testMatch` excludes it from Playwright runs.

# Local conventions
- Specs mock backend calls via `page.route` on `/bff/*` paths only, matching the BFF boundary — never `/api/v1/*`.
- Fixture JWTs use `alg: "none"` and a literal `"fixture"` signature segment (shape-valid, never cryptographically valid) — don't mistake these for realistic tokens if reusing this pattern.

# Local anti-patterns / gotchas
- `fixtures/vitest-environment.test.tsx` is easy to miscount as an e2e spec since it lives under `tests/fixtures/`; it only runs under the vitest unit-test command.
- The Playwright `webServer` builds and runs a production server, so timing/behavior differences from `next dev` won't show up locally without running the suite.
