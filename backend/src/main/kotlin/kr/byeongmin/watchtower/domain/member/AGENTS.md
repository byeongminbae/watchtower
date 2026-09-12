<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# Member slice

## Overview

- `Member` is the cross-domain aggregate anchor (referenced from `auth`, `payment`, `subscription`, `watch`). Created only via `Member.from(NaverProfileResponseExternalDto)` — no public constructor, no separate signup flow outside `auth`.
- `MemberService.getMember` is the only implemented member endpoint (`GET /api/v1/member/{memberId}`, `@OwnerOnly`); the rest of `MemberController` (payments, watches, profile update, delete, subscription get/cancel) are contract stubs returning empty responses.
- `MemberTokenIssuer` (not `AuthService`) owns Watchtower JWT creation and the member-side token bookkeeping (`completeSignIn` on sign-in, `rotateRefreshToken` on renew) — see also `auth/AGENTS.md`.
- `MemberRole` is a two-value enum (`NORMAL`, `ADMIN`); a `TODO` flags it should move to a table if role requirements grow.

## Where to look

- Profile read endpoint + stubs for payments/watches/profile-update/delete/subscription: `controller/MemberController.kt`.
- Implemented service logic (`getMember`, existence-then-fetch pattern): `service/MemberService.kt`.
- JWT issuance/rotation on sign-in and renew: `service/MemberTokenIssuer.kt`.
- Entity with sign-in/sign-out/refresh-token lifecycle methods: `entity/Member.kt`.
- Response shape for profile reads: `dto/MemberResponseDto.kt`.
- Role enum: `enums/MemberRole.kt`.
- Empty marker repository (no custom queries yet): `repository/MemberRepository.kt`.
- Test fixture for constructing a `Member` with a specific ID via reflection (private constructor): `backend/src/test/kotlin/.../support/MemberSupport.kt`.

## Local conventions

- `Member`'s constructor is `private`; all creation goes through the `from(...)` companion factory. Tests that need a specific `id` (constructor leaves `id = null`) use `MemberSupport.createMember`, which sets the `id` field via reflection — this is the established pattern for member fixtures in tests, not a one-off hack.
- `completeSignIn` updates both `refreshToken` and `lastSignInAt`; `rotateRefreshToken` updates only `refreshToken` (renew doesn't bump last-sign-in-time). Preserve this distinction if touching either.
- `MemberService.getMember` checks `existsById` before `findById(...).get()` rather than using `getOrNull()`/`Optional` directly — follow this two-step check-then-fetch style if adding similarly-shaped lookups here (contrast with `AuthService`, which uses `findById(...).getOrNull()` directly).

## Local anti-patterns / gotchas

- `MemberTokenIssuer.signIn`/`rotate` call `member.id.ifNullThrow()`, which throws `BusinessException(CommonError.NULL_CASTING_ERROR)` for a transient (unpersisted) `Member` — always persist before issuing tokens.
