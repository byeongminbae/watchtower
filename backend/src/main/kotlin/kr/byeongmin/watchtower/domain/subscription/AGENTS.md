<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# Subscription slice

## Overview

- Data-model-only slice: no controller, service, or repository exists here — only entities and enums. Treat any subscription-related HTTP contract as living elsewhere (member/payment controllers already stub subscription-shaped endpoints).
- Entities: `Plan` (purchasable tier definition), `Subscription` (a member's active plan period), `Benefit` + `PlanBenefit` (join table giving a plan an integer-valued benefit, e.g. watch quota).
- `PlanTier` enum: `FREE(0)`, `BASIC(10)`, `PRO(100)` — the private `level` field implies ordered comparison is intended but no comparison logic is implemented yet.
- `BenefitType` enum currently has a single value, `WATCH_AMOUNT`.

## Where to look

- Tier ordering: `enums/PlanTier.kt`.
- Benefit kind: `enums/BenefitType.kt`.
- Plan-to-benefit mapping (plan, benefit, integer value): `entity/PlanBenefit.kt`.
- Member's current plan period: `entity/Subscription.kt`.
- Plan pricing/availability window: `entity/Plan.kt`.
- Benefit type wrapper entity: `entity/Benefit.kt`.

## Local conventions

- A comment on `Plan` states the intended business rule: upgrading to a higher tier while subscribed is allowed, downgrading is not, but refunds for remaining days are allowed — none of this is enforced in code yet since there's no service layer.
- `Plan.availableUntil` is nullable (open-ended availability); `availableFrom` is not.
- `Subscription.member` is `@OneToOne`, unlike most other member associations in this codebase which are `@ManyToOne`.
