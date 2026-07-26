<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.
<!-- END:nextjs-agent-rules -->

# Overview

- Next.js 16.2 / React 19 / MUI 7 client using the App Router.
- `src/app` owns routes and layouts; `@/*` resolves to `src/*`.

# Where to look

- `src/app/`: public home/login plus protected `watches`, `mypage`, and `admin` trees.
- `src/components/common/`: `RequireAuth` and `RequireAdmin` route guards.
- `src/components/layout/` and `src/components/watch/`: shared shell and watch UI.
- `src/lib/AuthContext.tsx`: cookie/JWT session bootstrap, logout, member refresh.
- `src/lib/useApiData.ts`: fetch lifecycle, stale-request protection, and `refetch()`.
- `src/lib/api/`: `auth`, `member`, `watch`, `payment`, and `admin` API objects.
- `src/lib/api/client.ts`: the only transport boundary; `src/types/domain.ts` holds shared models.
- `src/theme/`: MUI `ThemeRegistry` (Emotion SSR) and the dark Watchtower theme.

# Local conventions

- Keep TypeScript strict/no-emit with bundler resolution and isolated modules.
- Use the `@/` alias instead of deep relative imports.
- Add `"use client"` only to components needing hooks, browser APIs, or event handlers.
- Root layout composes `ThemeRegistry`, `AuthProvider`, and `Header`; nested layouts enforce access.
- Route all endpoint traffic through `apiClient` and domain API objects; return typed `ApiEnvelope<T>`.
- Re-export domain API objects through `src/lib/api/index.ts`.
- Read auth from the `watchtower_jwt` cookie through existing JWT helpers, not a second cache.
- Pages commonly call `useApiData(...)` and manually `refetch()` after mutations.
- Prefer MUI components and `sx`; extend `src/theme/theme.ts` for shared visual tokens.
- Current route leaves are client components; there are no route handlers,
  middleware, or server actions.

# Local anti-patterns / gotchas

- Do not invent direct `fetch` calls; this bypasses credentials, API logging, and 401 renewal.
- `apiClient` retries one 401 through `/api/v1/auth/renew`; avoid custom renewal loops.
- Error parsing currently expects `{ error: { code, message } }`; verify backend changes before relying on it.
- API console logging includes raw headers and bodies and is enabled in
  production by default; disable with `NEXT_PUBLIC_API_LOG=false`.
- JWT cookie name, readability, and claims are inferred contracts; verify them
  before extending auth behavior.
- `useApiData` intentionally owns stale-request protection and has a controlled
  exhaustive-deps suppression; do not casually duplicate that hook pattern.
