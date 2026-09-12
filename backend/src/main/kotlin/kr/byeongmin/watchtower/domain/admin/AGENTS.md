<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# Admin slice

## Overview

- Admin-only surface: `AdminController` (watch/payment/user moderation) and `StatsController` (dashboard aggregates).
- Both controllers are `@AdminOnly`-guarded at class level; no per-method override.
- No service, repository, or entity layer exists yet — every handler is a contract stub returning `SuccessResponse()`/`SuccessDataResponse("")`.

## Where to look

- Moderation endpoints (watch status, payment cancel, user search/role/status): `controller/AdminController.kt`.
- Dashboard stat endpoints (watches/payments/users/auth): `controller/StatsController.kt`.

## Local conventions

- Routes: `AdminController` under `/api/v1/admin`, `StatsController` under `/api/v1/admin/stats`.
