<!-- Parent: ../../../../../../../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# Global infrastructure

## Overview

- Shared plumbing used by every domain slice: response envelopes, error taxonomy, exception handling, JWT/security, time helpers, and Spring config beans.
- Two independent error-to-response paths exist: `GlobalExceptionHandler` (`@RestControllerAdvice`, catches `BusinessException` thrown inside normal controller execution) and `SecurityExceptionHandlerFilter` (a servlet filter that also implements `AuthenticationEntryPoint`/`AccessDeniedHandler`, handling auth failures that happen before/outside the Spring MVC dispatch, e.g. inside other filters).

## Where to look

- Response envelope types (`Response` interface, `SuccessResponse`, `SuccessDataResponse<T>`, `ErrorResponse`): `response/`.
- Error code enums (`Error` interface, `CommonError`, `AuthError`): `error/`.
- Exception type and its one handler: `exception/BusinessException.kt`, `exception/GlobalExceptionHandler.kt`.
- Time helpers (`entityTime()` = UTC, `debuggingTime()` = Asia/Seoul): `utils/TimeUtil.kt`.
- Null-to-exception helper used across entities/services (`T?.ifNullThrow()`): `utils/GlobalExtensions.kt`.
- Outbound HTTP client bean with logging + error-mapping status handler: `config/RestClientConfig.kt`.
- Swagger/OpenAPI bearer-auth security scheme: `config/OpenApiConfig.kt`.
- Audit/soft-delete mapped superclasses: `entity/Base.kt` (mutable audit fields + `deletedAt`), `entity/HistoryBase.kt` (immutable `createdAt` only, for append-only history tables).
- Method-security annotations (`@AuthenticatedUser`, `@AdminOnly`, `@OwnerOnly`, `@MemberId`): `security/EndpointAuthorizationAnnotations.kt`.
- Authenticated-principal shape (`memberId`, `role`): `security/MemberPrinciple.kt`.
- JWT create/validate/parse: `security/JwtProvider.kt`.
- Bearer-token extraction + `SecurityContext` population filter: `security/JwtAuthenticationFilter.kt`.
- Auth/access-denied JSON error responses + a BusinessException-catching filter wrapper: `security/SecurityExceptionHandlerFilter.kt`.
- Filter chain wiring, CORS, and the `authorizeHttpRequests().anyRequest().permitAll()` policy: `security/SecurityConfig.kt`.

## Local conventions

- `RestClientConfig`'s `RestClient` bean throws `BusinessException(CommonError.EXTERNAL_API_ERROR)` for any non-2xx response via `defaultStatusHandler`; callers of the shared `RestClient` (e.g. `AuthService`) don't need their own status-code handling for that case, only for null-body handling.
- `SecurityExceptionHandlerFilter` is registered twice in `SecurityConfig`: once as the `authenticationEntryPoint`/`accessDeniedHandler` bean (handles `AuthenticationException`/`AccessDeniedException` raised by Spring Security itself), and once as an actual filter added via `addFilterBefore` (its own `doFilterInternal` catches `BusinessException` thrown by filters running after it, e.g. `JwtAuthenticationFilter`). Both roles matter — don't assume one registration is redundant with the other.
- `JwtAuthenticationFilter.createUsernamePasswordAuthenticationToken` swallows all parse failures via `runCatching {}.getOrNull()` and simply skips setting authentication rather than rejecting the request; actual token validity is enforced separately by `jwtProvider.validateToken(token)` a few lines earlier, which throws `BusinessException(AuthError.EXPIRED_TOKEN | INVALID_TOKEN)`.
- `Base.deletedAt` is set via `delete()` using `LocalDateTime.now()` (system default zone), not `TimeUtil.entityTime()` (UTC) — inconsistent with the rest of `Base`'s audit fields, which are UTC via `@CreatedDate`/`@LastModifiedDate` auditing infrastructure.

## Local anti-patterns / gotchas

- `GlobalExceptionHandler` only has an `@ExceptionHandler(BusinessException::class)` method — verified directly in `exception/GlobalExceptionHandler.kt`. There is no handler for `MethodArgumentNotValidException`/`BindException` or other generic Spring validation/binding errors; those currently propagate as unhandled 500s rather than a structured `ErrorResponse`.
- `SecurityConfig.securityFilterChain` sets `.authorizeHttpRequests { it.anyRequest().permitAll() }` — every endpoint is open at the Spring Security layer regardless of `@AuthenticatedUser`/`@AdminOnly`/`@OwnerOnly`; those annotations rely entirely on `@EnableMethodSecurity`'s `@PreAuthorize` evaluation, not on the filter chain's own authorization rules.
- Flyway migrations do exist under `src/main/resources/db/migration/` (`V1__initial_schema.sql` through `V4__update_member_sign_in_fields.sql`) — the claim in root `AGENTS.md` that this directory is absent is stale as of this check.
