<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# Payment slice

## Overview

- `PaymentController` (`/api/v1/payments`) is a contract stub: `getPayment` and `confirmTossPayment` (both `@AuthenticatedUser`) return empty `SuccessDataResponse<String>`.
- `PaymentHistory` and `PaymentCancelHistory` are the only two payment entities; a class comment on `PaymentHistory` explicitly rejects a richer "extensible payment method/target" model as premature complexity — Toss is expected to stay the only payment method and `Plan` the only purchasable target for the foreseeable future.
- No service or repository layer exists for this slice yet.

## Where to look

- HTTP contract stubs: `controller/PaymentController.kt`.
- Successful payment record (member, plan snapshot fields, raw Toss response): `entity/PaymentHistory.kt`.
- Cancellation record (amount, reason, Toss transaction key, raw response): `entity/PaymentCancelHistory.kt`.

## Local conventions

- Both entities extend `HistoryBase` (append-only audit trail, no soft delete) and store the full raw Toss response in a `TEXT` column (`tossRawResponse`) alongside parsed fields.
- `PaymentHistory` denormalizes plan fields (`planName`, `planPrice`, `planDurationDays`, `planTier`) at time of purchase rather than only referencing `Plan`, so historical records aren't affected by later plan changes.
- `PaymentCancelHistory` links back to `PaymentHistory` via `@ManyToOne`, not the other way — one payment can have multiple cancel records.

## Local anti-patterns / gotchas

- A `TODO` on `PaymentHistory` flags that coupon/reward-point usage fields still need a home; don't assume the current fields are the final schema.
