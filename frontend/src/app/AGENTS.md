<!-- Parent: ../../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# App Router route tree

# Overview
- Next.js App Router leaves under `src/app`; `layout.tsx` composes `ThemeRegistry`, `AuthProvider`, and `Header` around every route, and injects `react-grab`/`react-scan` dev-only debug scripts in development.
- `page.tsx` is the public landing page (hero, `HotUrlSection`, `LandingStory`); CTA links go to `/watches` if logged in, `/login` otherwise.
- Route groups: `admin/` (admin-only), `bff/` (server-side proxy to the backend — see `bff/AGENTS.md`), `login/` (public OAuth entry), `mypage/` (authenticated user area), `watches/` (authenticated watch management).

# Where to look
- `admin/layout.tsx`: wraps children in `RequireAdmin` (login + `principal.role === "ADMIN"`).
- `admin/page.tsx`: dashboard — parallel-fetches watch/user/payment/auth stats via `adminApi`, renders stat cards and a `LineChart` trend.
- `admin/payments/page.tsx`, `admin/users/page.tsx`, `admin/watches/page.tsx`: search + table + action-menu/dialog admin CRUD screens, all backed by `adminApi` (currently stubbed — see `src/lib/AGENTS.md`).
- `bff/`: Next.js Route Handlers acting as the backend-for-frontend proxy — see `bff/AGENTS.md`.
- `login/page.tsx`: `LoginContent` wrapped in `React.Suspense` (uses `useSearchParams`); starts the Naver OAuth transaction and redirects to the Naver authorize URL.
- `login/callback/page.tsx`: `OAuthCallbackPage` — captures/cleans the `code`/`state` query on mount, validates the stored OAuth transaction, exchanges the code, and calls `acceptSession`.
- `mypage/layout.tsx`: wraps children in `RequireAuth` (login only).
- `mypage/page.tsx`: profile view/edit, session renew button, account deletion dialog.
- `mypage/payments/page.tsx`, `mypage/subscription/page.tsx`: payment history table and subscription/plan management (cancel dialog).
- `watches/layout.tsx`: wraps children in `RequireAuth`.
- `watches/page.tsx`: split list/detail view — derives the selected watch at render time (no effect), fetches that watch's snapshots via a second `useApiData` keyed on `selectedWatch?.id`.
- `watches/[watchId]/page.tsx`: standalone single-watch view (shareable link), reuses `WatchDetailPanel`.

# Local conventions
- `login/` and `login/callback/` are the only routes with no guard layout (must stay reachable while logged out).
- `admin`, `mypage`, and `watches` layouts are one-line client components whose only job is wrapping children in a guard from `src/components/common/`.
- Co-located `page.test.tsx` vitest files exist for `admin/payments`, `login`, `login/callback`, `mypage`, `mypage/payments`, `mypage/subscription`, and `watches`; `admin/page.tsx`, `admin/users/page.tsx`, `admin/watches/page.tsx`, and `watches/[watchId]/page.tsx` currently have none.

# Local anti-patterns / gotchas
- `watches/[watchId]/page.tsx` calls `useApiData` for both the watch and its snapshots using `Number(params.watchId)` *before* checking `Number.isNaN(watchId)` and calling `notFound()` — an invalid id still fires both fetches with `NaN` before the page bails out.
- `watches/page.tsx`'s selected-watch derivation ("effect 없이 렌더 시점에 파생") intentionally avoids an effect to prevent a render cascade; don't "fix" it into a `useEffect` + `useState` pair.
