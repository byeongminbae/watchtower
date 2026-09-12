<!-- Generated: 2026-07-25 | Updated: 2026-09-12 -->

# PROJECT KNOWLEDGE BASE

**Generated:** 2026-07-25 (Asia/Seoul)
**Updated:** 2026-09-12 (Asia/Seoul) — deep hierarchical AGENTS.md pass; several facts below refreshed, see NOTES.
**Commit:** 1ba1a42 (content below); tree refreshed at 4646bd5
**Branch:** feature/39-agentsmd-문서-트리-추가

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

Each directory above (and several nested ones) carries its own `AGENTS.md` with
a `<!-- Parent: ... -->` comment pointing back up the tree — start at the
nearest one to where you're editing rather than re-deriving context from here.
Notably: `backend/.../domain/{admin,auth,member,payment,subscription,watch}/AGENTS.md`,
`backend/.../global/AGENTS.md`, `frontend/src/app/AGENTS.md` (+ `app/bff/AGENTS.md`
for the Next BFF proxy layer), `frontend/src/components/AGENTS.md`,
`frontend/src/lib/AGENTS.md`, and `frontend/tests/AGENTS.md`.

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
- Do not assume CI covers tests. Backend deployment builds the image with
  `-x test`; frontend now has vitest unit tests (co-located `*.test.ts(x)`)
  and a Playwright e2e suite (`frontend/tests/e2e/`), but the deploy workflow
  itself does not run either suite — it only gates on a Docker `HEALTHCHECK`
  poll after building the image.

## USER-GOVERNED POLICY

- `README.md` (and other documentation Markdown files) must **not** be edited
  unless the user explicitly approves it in the current request.
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
- `application-prod.yaml` exists and only imports AWS Parameter Store
  (`aws-parameterstore:/watchtower/backend/prod/`) — all other prod settings
  live in Parameter Store, not in the repo.
- Flyway migrations exist at `backend/src/main/resources/db/migration/`
  (`V1__initial_schema.sql` through `V4__update_member_sign_in_fields.sql`) —
  earlier notes claiming this directory was absent are outdated.
- Backend has both the context-load smoke test and real unit/service tests
  under `backend/src/test/`; frontend has vitest unit tests co-located with
  source and a Playwright e2e suite under `frontend/tests/e2e/`. Earlier notes
  claiming "no tests" on either side are outdated.
- `HtmlCondition`/`RegexCondition`/`KeywordCondition` each now use their own
  distinct `@DiscriminatorValue` (`HTML`/`REGEX`/`KEYWORD`) — the previously
  noted defect where `HtmlCondition` and `RegexCondition` shared the `REGEX`
  discriminator has been fixed; this note is now stale/historical.
- Backend success and frontend success envelopes align around `data`, but
  backend errors expose `statusCode/message` while the frontend expects nested
  `error.code/message`.
