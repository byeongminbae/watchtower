# Verification: frontend/AGENTS.md

Date: 2026-07-25 (Asia/Seoul)

## Scenario

Verify the frontend init-deep guidance file is present, within the requested size, preserves the mandatory Next.js block, contains the requested frontend-local sections, and is the only target file changed by this subtask.

## Commands and binary observables

1. `wc -l frontend/AGENTS.md`
   - Output: `47 frontend/AGENTS.md`
   - Judgment: pass; line count is within 30–80.

2. Python marker/section assertion against `frontend/AGENTS.md`:

   ```text
   line_count= 47
   preserved_block_exact= True
   marker_count= 1 1
   overview=True
   where_to_look=True
   local_conventions=True
   local_anti_patterns=True
   ```

   - Judgment: pass; the preserved block matches exactly once and all required sections exist. The commands section is optional and is not present in the current shared-tree revision.

3. `git status --short -- frontend/AGENTS.md`
   - Output: `?? frontend/AGENTS.md`
   - Judgment: pass for this subtask; the target is the only path selected by the scoped status check. Other untracked AGENTS files belong to concurrent repository work.

4. `npm run lint` from `frontend/`
   - Exit code: `0`.
   - Output is ESLint CLI help rather than a lint report, so this is recorded as informational, not as a substantive lint pass.

5. `npx eslint src` from `frontend/`
   - Exit code: `2`; ESLint could not resolve the local `eslint` package from `eslint.config.mjs`.
   - Judgment: unrelated environment/dependency issue; no source or documentation change was made to mask it.

## Paths inspected

- `frontend/AGENTS.md`
- `frontend/package.json`
- `frontend/tsconfig.json`
- `frontend/src/app/`
- `frontend/src/lib/api/`
- `frontend/src/lib/AuthContext.tsx`
- `frontend/src/lib/useApiData.ts`
- `frontend/src/components/common/`
- `frontend/src/components/layout/Header.tsx`
- `frontend/src/theme/`
- `frontend/src/lib/apiLogger.ts`
- `frontend/README.md`

The shared tree was subsequently deduplicated by the parent agent; the final verification above reflects the resulting 47-line document rather than the earlier 52-line draft.
