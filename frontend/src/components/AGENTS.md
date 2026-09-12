<!-- Parent: ../../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# Components

# Overview
- Presentational and guard components grouped by folder: `common/` (route guards, shared marks), `home/` (landing page), `layout/` (app shell), `watch/` (watch feature UI).

# Where to look
- `common/LighthouseMark.tsx`: the lighthouse logo (`next/image` wrapper), reused in `Header`, login, and OAuth-callback screens.
- `common/RequireAuth.tsx`: login-only route guard — redirects to `/login?next=<path>` once session init finishes; shows a spinner while initializing.
- `common/RequireAdmin.tsx`: login + `principal.role === "ADMIN"` guard — redirects like `RequireAuth`, but shows an in-place "접근 권한이 없습니다" message (no redirect) if logged in without admin role.
- `home/LandingStory.tsx`: static two-section landing copy (common pain points, how-it-works steps) — no data fetching, pure presentation.
- `layout/Header.tsx`: sticky `AppBar` — nav items and avatar menu contents both branch on `isAdmin`; renders a login button when logged out.
- `watch/CreateWatchDialog.tsx`: new-watch form dialog (name, URL, interval slider, one condition, `includeInStat` toggle).
- `watch/AddConditionDialog.tsx`: adds one condition (`HTML_FULL`/`KEYWORD`/`REGEX`) to an existing watch; enforces the 3-condition cap client-side.
- `watch/WatchDetailPanel.tsx`: the per-watch detail view — status toggle, test notification, condition chips (add/delete with optimistic rollback), embeds `SnapshotTimeline`.
- `watch/SnapshotTimeline.tsx`: renders a watch's snapshot history with a diff/full view toggle.
- `watch/WatchListItem.tsx`: one row in the watches sidebar list (used by `app/watches/page.tsx`).
- `watch/WatchStatusDot.tsx`: single source of truth for `WatchStatus` → label/color/description, rendered as either a bare dot or a `Chip`.
- `watch/HotUrlSection.tsx`: landing-page widget that auto-rotates through trending watched URLs every second (`ROTATION_INTERVAL_MS`).

# Local conventions
- `common/RequireAuth`, `common/RequireAdmin`, and `layout/Header` each have a co-located `*.test.tsx`; nothing under `watch/` or `home/` does.

# Local anti-patterns / gotchas
- `SnapshotTimeline`'s screenshot area is a placeholder box (caption text only, e.g. "변경 강조 스크린샷") — no real screenshot rendering exists yet; don't assume an `<img>` is there.
- `WatchDetailPanel` seeds its `conditions` state from `watch.conditions` once and separately refetches via `getWatchConditions` in an effect keyed on `watch.id`; callers **must** pass `key={watch.id}` (as `watches/page.tsx` and `watches/[watchId]/page.tsx` do) or stale condition state leaks across watches on selection change.
- `RequireAuth`/`RequireAdmin` build the post-login `next` redirect target from `globalThis.location` directly rather than `usePathname`/`useSearchParams` — keep both guards consistent if this is ever refactored.
