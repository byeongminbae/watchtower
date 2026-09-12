<!-- Parent: ../../../../../../../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# Domain slices

## Overview

- Six vertical slices, package-by-domain. Most are still HTTP-contract stubs (`SuccessResponse()`/`SuccessDataResponse("")`); `auth` and `member` have real service logic.

## Where to look

- [`admin/AGENTS.md`](admin/AGENTS.md) — admin-only moderation (watch/payment/user) and dashboard stats; controller stubs only, no service/repository.
- [`auth/AGENTS.md`](auth/AGENTS.md) — Naver OAuth login flow (URL issuance, callback token/profile exchange, member link-or-create, Watchtower JWT issuance/renew/logout); the most fully implemented slice.
- [`member/AGENTS.md`](member/AGENTS.md) — `Member` entity (cross-domain aggregate anchor), profile read (implemented), JWT issuance/rotation via `MemberTokenIssuer`; most controller endpoints are still stubs.
- [`payment/AGENTS.md`](payment/AGENTS.md) — Toss payment/cancel history entities; controller stubs only, no service/repository.
- [`subscription/AGENTS.md`](subscription/AGENTS.md) — plan/benefit/subscription data model only; no controller, service, or repository exists here yet.
- [`watch/AGENTS.md`](watch/AGENTS.md) — largest slice: watched URLs, trigger conditions (regex/keyword/html), fetch history, change snapshots; controller stubs only, no service/repository.
