# PROJECT KNOWLEDGE BASE

**Generated:** 2026-07-25 (Asia/Seoul)
**Commit:** 1ba1a42
**Branch:** feature/11-프론트엔드-기초-작업

## OVERVIEW

Watchtower is a two-application web service: a Kotlin/Spring Boot API and a
Next.js/React client. Deployment targets AWS ECR/ECS through a manually
dispatched GitHub Actions workflow.

## STRUCTURE

```text
watchtower/
├── backend/    # Spring Boot API, persistence model, Docker image
├── frontend/   # Next App Router UI and typed API client
├── ecs/        # ECS task definitions for ports 8080 and 3000
├── .github/    # Manual service/environment deployment workflow
└── README.md   # Project/Wiki links only
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Start the API | `backend/.../WatchtowerApplication.kt` | Enables JPA auditing |
| Add an HTTP endpoint | `backend/.../domain/*/controller/` | Existing handlers are contract stubs |
| Change persistence | `backend/.../domain/*/entity/` | Shared bases live under `global/entity` |
| Change global API envelopes/errors | `backend/.../global/{response,error,exception}/` | Frontend error shape currently differs |
| Add/change a page | `frontend/src/app/` | App Router; protected areas use nested layouts |
| Change auth/session behavior | `frontend/src/lib/AuthContext.tsx`, `frontend/src/lib/api/client.ts` | Cookie/JWT contract is provisional |
| Change frontend API contracts | `frontend/src/lib/api/`, `frontend/src/types/domain.ts` | Compare with backend controller signatures |
| Change deployment | `.github/workflows/watchtower-deploy.yml`, `ecs/` | Workflow chooses one service and environment |

## CODE MAP

CodeGraph is indexed; TypeScript and Kotlin LSP servers are configured but not
installed. Reference counts below come from CodeGraph/source relationships.

| Symbol | Type | Location | Refs | Role |
|--------|------|----------|------|------|
| `main` | Function | `backend/.../WatchtowerApplication.kt` | Framework entry | Launches Spring component scan |
| `SuccessResponse<T>` | Class | `backend/.../global/response/SuccessResponse.kt` | 6 controllers | Shared success envelope |
| `Base` | Mapped superclass | `backend/.../global/entity/Base.kt` | 9 entities | Audit fields and soft delete |
| `Member` | Entity | `backend/.../domain/member/entity/Member.kt` | 6 domain files | Cross-domain aggregate anchor |
| `RootLayout` | Component | `frontend/src/app/layout.tsx` | Framework entry | Composes theme, auth, header, routes |
| `AuthProvider` | Component | `frontend/src/lib/AuthContext.tsx` | Root layout | Cookie-to-member session state |
| `apiClient` | Object | `frontend/src/lib/api/client.ts` | ~41 call sites | Fetch, bearer auth, renewal, retry |
| `useApiData` | Hook | `frontend/src/lib/useApiData.ts` | ~23 refs | Shared fetch lifecycle/refetch pattern |
| `domain.ts` | Contract module | `frontend/src/types/domain.ts` | ~16 importers | 13 shared domain exports |

## CONVENTIONS

- There is no root build system; run Gradle and npm from their package roots.
- Backend routes use `/api/v1/*`; frontend domain API objects mirror those
  route groups through one transport client.
- Backend code is package-by-domain plus a small `global` infrastructure tree.
- Frontend pages call API objects directly and manually `refetch()` after
  mutations; no query/cache library is present.
- User-facing strings and many explanatory comments are Korean.
- `NEXT_PUBLIC_API_BASE_URL` is a frontend build-time value, not an ECS
  runtime-only setting.

## ANTI-PATTERNS (THIS PROJECT)

- Do not treat frontend request/response types as implemented backend
  contracts. Controllers currently accept little input and return empty
  `SuccessResponse<String>` placeholders.
- Do not rely on current auth as enforcement: backend security permits every
  request, while the frontend assumes a readable `watchtower_jwt` cookie,
  specific claims, renewal-by-cookie, and credentialed CORS.
- Do not select frontend deployment expecting it to complete: the workflow
  requires `frontend/Dockerfile`, which does not exist.
- Do not assume CI covers tests. Backend deployment explicitly uses `-x test`;
  frontend has no test framework or test script.

## UNIQUE STYLES

- Authenticated client areas are guarded at route-layout level:
  `watches`/`mypage` use `RequireAuth`; `admin` uses `RequireAdmin`.
- Backend domain controllers and entities exist before service/repository
  layers; preserve that distinction when judging what is real behavior.
- ECS task definitions use EC2 bridge networking with dynamic host ports and
  separate backend/frontend container names.

## COMMANDS

```bash
# Backend: Java 25 required; wrapper is currently not executable
cd backend
chmod +x ./gradlew
./gradlew clean build
./gradlew test
./gradlew bootRun --args='--spring.profiles.active=local'

# Frontend: CI uses Node 20
cd frontend
npm ci
npm run lint
npm run build
npm run dev

# Backend image
docker build --build-arg SPRING_PROFILE=dev \
  -t watchtower-backend-dev:tag backend
```

## NOTES

- Dev/local Spring profiles import AWS Parameter Store and use PostgreSQL with
  `ddl-auto=create`; the default profile uses `validate`.
- No `application-prod.yaml` exists. Production depends on common/external
  configuration.
- The only backend test is a Spring context-load smoke test; frontend has no
  tests.
- `HtmlCondition` and `RegexCondition` currently share the `REGEX` JPA
  discriminator; treat this as a pre-existing defect, not a convention.
- Backend success and frontend success envelopes align around `data`, but
  backend errors expose `statusCode/message` while the frontend expects nested
  `error.code/message`.
