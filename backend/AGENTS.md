<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# Backend guide

## Overview

- Kotlin 2.3.21 / Spring Boot 4.1.0; Java toolchain 25.
- Application entry: `src/main/kotlin/kr/byeongmin/watchtower/WatchtowerApplication.kt`.
- Package root is `kr.byeongmin.watchtower`.
- Domain code is organized as vertical slices under `domain/*`; shared plumbing under `global/*`.
- `watch` is the largest slice; `Member` is the cross-domain aggregate anchor.

## Where to look

- Add/change HTTP handlers in `src/main/kotlin/.../domain/*/controller/`.
- Domain entities and enums live beside each domain (`domain/*/entity`, `domain/*/enums`).
- Shared response envelopes: `global/response/`; errors and exception mapping: `global/error/`, `global/exception/`.
- Security policy: `global/security/SecurityConfig.kt`.
- Audit/soft-delete bases: `global/entity/Base.kt` and `global/entity/HistoryBase.kt`.
- Time conversion helpers: `global/utils/TimeUtil.kt`.
- JUnit 5 tests: `src/test/kotlin/`; current naming is `*Tests.kt`.

## Local conventions

- Kotlin compilation uses strict JSR-305 and parameter-property annotation defaults.
- Spring/JPA plugins open `@Entity`, `@MappedSuperclass`, and `@Embeddable` classes.
- Keep changes inside the owning domain slice; domain enums stay domain-local.
- IDs are nullable constructor properties backed by explicit sequence generators
  with `allocationSize = 30`.
- Associations are mostly unidirectional and `LAZY`; preserve existing fetch direction unless contract requires otherwise.
- Entities inherit `Base` or `HistoryBase` for auditing fields and soft-delete state.
- `OAuth` and `WatchCondition` use `JOINED` inheritance with discriminator values.
- Wrap successful controller payloads in `SuccessResponse`; route prefixes remain `/api/v1`.
- Use 4-space Kotlin indentation and trailing commas in multiline declarations.
- Keep user-facing messages/comments Korean where adjacent code does.
- `TimeUtil.entityTime()` is UTC; response/debug timestamps use Asia/Seoul.

## Subdirectories

| Directory | AGENTS.md |
|-----------|-----------|
| `src/main/kotlin/.../domain/` | `src/main/kotlin/kr/byeongmin/watchtower/domain/AGENTS.md` (index over the 6 domain slices) |
| `src/main/kotlin/.../domain/admin/` | `.../domain/admin/AGENTS.md` |
| `src/main/kotlin/.../domain/auth/` | `.../domain/auth/AGENTS.md` (Naver OAuth login flow) |
| `src/main/kotlin/.../domain/member/` | `.../domain/member/AGENTS.md` |
| `src/main/kotlin/.../domain/payment/` | `.../domain/payment/AGENTS.md` |
| `src/main/kotlin/.../domain/subscription/` | `.../domain/subscription/AGENTS.md` |
| `src/main/kotlin/.../domain/watch/` | `.../domain/watch/AGENTS.md` (largest slice) |
| `src/main/kotlin/.../global/` | `src/main/kotlin/kr/byeongmin/watchtower/global/AGENTS.md` |

## Local anti-patterns / gotchas

- `HtmlCondition`/`RegexCondition`/`KeywordCondition` now each use a distinct `@DiscriminatorValue` (`HTML`/`REGEX`/`KEYWORD`); the earlier note about `HtmlCondition` and `RegexCondition` sharing `REGEX` is fixed and stale.
- Flyway migrations exist at `src/main/resources/db/migration/` (`V1__initial_schema.sql` through `V4__update_member_sign_in_fields.sql`); the earlier "absent" note is stale.
- Soft deletion is only a `deletedAt` marker; no automatic query filter is present.
- `GlobalExceptionHandler` handles `BusinessException`, not generic binding or
  validation failures.
