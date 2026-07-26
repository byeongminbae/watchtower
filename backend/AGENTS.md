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

## Local anti-patterns / gotchas

- `HtmlCondition` and `RegexCondition` both use the JPA `REGEX` discriminator; this is a known defect, not a pattern to copy.
- Flyway is a dependency, but `src/main/resources/db/migration/` is absent.
- Soft deletion is only a `deletedAt` marker; no automatic query filter is present.
- `GlobalExceptionHandler` handles `BusinessException`, not generic binding or
  validation failures.
