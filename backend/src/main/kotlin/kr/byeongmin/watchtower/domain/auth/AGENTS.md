<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# Auth slice

## Overview

- Implements Naver OAuth login end-to-end: URL issuance -> callback (token exchange + profile fetch + member link/create) -> Watchtower JWT issuance -> token renewal/logout. This is one of the few slices with a real (non-stub) service.
- Flow lives in `service/AuthService.kt`, called from `controller/AuthController.kt` (`/api/v1/auth`).
- `getNaverSignInUrl` builds the Naver authorize URL; `signInWithNaverCallback` exchanges the code for a Naver token, fetches the Naver profile, then either signs in an existing linked member (`naverMemberSignIn`) or creates one (`naverMemberSignUpThenSignIn`).
- Member creation and Watchtower token issuance are delegated out of this slice: `Member.from(...)` (member domain) and `MemberTokenIssuer` (member domain) do the actual work: `AuthService` only orchestrates.
- `renewToken`/`logout` operate on the Watchtower-issued JWT, not the Naver token.

## Where to look

- OAuth endpoints (url/callback/renew/logout): `controller/AuthController.kt`.
- Orchestration logic: `service/AuthService.kt`.
- Naver OAuth link record (per-member, one row per provider): `entity/NaverOAuth.kt`, base class `entity/OAuth.kt`.
- Naver-side lookup by provider id: `repository/NaverOAuthRepository.kt`.
- External Naver API response shapes: `dto/NaverTokenResponseExternalDto.kt`, `dto/NaverProfileResponseExternalDto.kt`.
- Watchtower's own issued-token response shape: `dto/MemberTokenResponseDto.kt`.
- Expected behavior reference (mocked Naver HTTP calls via `MockRestServiceServer`): `backend/src/test/kotlin/.../domain/auth/service/AuthServiceTest.kt`.

## Local conventions

- `AuthService` calls Naver directly with the injected `RestClient` (see `global/AGENTS.md` for the shared error-mapping behavior on non-2xx) rather than through a dedicated Naver client abstraction; a `TODO` in the file flags that this should move into a separate `NaverAuthService` later.
- `NaverOAuthRepository.existsByProviderId` decides sign-in vs. sign-up branching in `signInWithNaverCallback`.
- `NaverOAuth`/`OAuth` use `JOINED` inheritance with `@DiscriminatorValue("NAVER")`; `OAuth` is the only other provider-agnostic base besides `NaverOAuth` today — no other provider is implemented.
- Both `getNaverMemberToken` and `getNaverMemberProfile` throw `BusinessException(CommonError.EXTERNAL_API_ERROR)` on a null body, in addition to whatever `RestClientConfig`'s status handler already raises for non-2xx responses.

## Local anti-patterns / gotchas

- `renewToken` and `logout` both look up the member by ID via `memberRepository.findById(...).getOrNull()` and throw `RESOURCE_NOT_FOUND` if absent, rather than trusting the JWT-derived ID — this is intentional defensiveness against deleted/soft-deleted members, not redundant.
