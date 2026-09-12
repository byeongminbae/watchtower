<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# .github

## Overview

- One manually-dispatched deploy workflow: `workflows/watchtower-deploy.yml`. No CI-on-push workflows exist (no lint/test-on-PR pipeline in this repo).
- Deploy is `workflow_dispatch` only, with two required inputs: `service` (`backend` | `frontend` | `caddy`) and `environment` (`dev` | `prod`, ignored for `caddy` since it's a shared service).

## Where to look

- Change deploy steps, image build args, or the pre-deploy health-check gate: `workflows/watchtower-deploy.yml`.
- Change what gets deployed: the corresponding `ecs/task-definition.{service}.json` (rendered by this workflow, not stored here — see `../ecs/AGENTS.md`).

## Local conventions

- Backend and frontend images are built with `docker build -f "${SERVICE}/Dockerfile" ... "./${SERVICE}"` and tagged `{service}-{env}-{sha}`; `caddy` skips the build step entirely and deploys the upstream `caddy:2-alpine` image as-is.
- Before pushing to ECR, the workflow runs the freshly built image locally (with a throwaway Postgres container for backend) and polls Docker's own `HEALTHCHECK` until `healthy`/`unhealthy`/timeout — a failing health check blocks the deploy before anything reaches ECS.
- Frontend builds pass `INTERNAL_API_BASE_URL=http://back-${DEPLOY_ENV}:8080` as a build arg, distinct from any `NEXT_PUBLIC_*` browser-facing base URL.

## Local anti-patterns / gotchas

- This workflow builds `-x test`-equivalent for backend (see root/backend AGENTS.md: deploy does not run the Kotlin test suite) — the Docker-level health check is the only automated gate before a backend deploy, not `./gradlew test`.
- `ECS_SERVICE` naming has a caddy special case (`workflow_dispatch` env var expression in the workflow); if you add a fourth service, replicate that ternary rather than assuming the generic `watchtower-{service}-{env}-task-service` pattern always applies.
