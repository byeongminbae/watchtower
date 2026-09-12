<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# Watch slice

## Overview

- Largest domain slice: watched URLs (`Watch`), their trigger conditions (`WatchCondition` and three subtypes), fetch/notification audit trail (`WatchRequestHistory`), and change snapshots (`WatchSnapshot`).
- `WatchController` (`/api/v1/watches`) is entirely contract stubs — create/get/update/delete watch, conditions CRUD, snapshot listing/detail, test notification, trending list. No service or repository layer exists yet.
- `Watch` carries scheduling/status fields (`intervalSeconds`, `status: WatchStatus`, `includeInStat`) plus mutable tracking fields (`faviconUrl`, `lastFetchedAt`, `lastNotifiedAt`) updated via `fetched()`/`notified()`/`updateFaviconUrl()`.
- `WatchStatus`: `RUNNING`, `PAUSED`, `PAYMENT_REQUIRED`, `ILLEGAL_SUSPENDED` — the last covers admin-driven policy-violation suspension (see `AdminController.updateWatchStatus`).

## Where to look

- HTTP contract stubs for watches, conditions, snapshots: `controller/WatchController.kt`.
- Core watch entity + tracking-field mutators: `entity/Watch.kt`.
- Condition base (`JOINED` inheritance root): `entity/WatchCondition.kt`.
- Condition subtypes: `entity/RegexCondition.kt`, `entity/KeywordCondition.kt`, `entity/HtmlCondition.kt`.
- Per-fetch audit record (who/when/url/http status): `entity/WatchRequestHistory.kt`.
- Detected-change record (HTML/screenshot URLs + diffs + AI summary): `entity/WatchSnapshot.kt`.
- Status enum: `enums/WatchStatus.kt`.

## Local conventions

- `Watch`'s constructor is `private`; tracking fields (`faviconUrl`, `lastFetchedAt`, `lastNotifiedAt`) are `private var` mutated only through the entity's own methods — no direct external mutation path exists (or is expected) for these.
- `WatchCondition.watch` is `protected`, not `private` — subtypes can reference it directly (none currently do beyond the constructor).
- `WatchRequestHistory.triggeredBy` is nullable: `null` means an automatic (scheduled) trigger, non-null means an admin/member manually triggered the check.

## Local anti-patterns / gotchas

- The three `WatchCondition` subtypes' `@DiscriminatorValue` annotations were checked directly: `RegexCondition` uses `"REGEX"` (`entity/RegexCondition.kt:7`), `KeywordCondition` uses `"KEYWORD"` (`entity/KeywordCondition.kt:7`), `HtmlCondition` uses `"HTML"` (`entity/HtmlCondition.kt:7`). All three discriminator values are distinct — the previously documented defect (`HtmlCondition`/`RegexCondition` sharing `"REGEX"`) is **not present** in the current code; treat that claim in `backend/AGENTS.md` and root `AGENTS.md` as stale.
