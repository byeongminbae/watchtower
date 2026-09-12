<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-09-12 | Updated: 2026-09-12 -->

# ecs

## Overview

- Three ECS task definitions, one per container, deployed independently by `.github/workflows/watchtower-deploy.yml`.
- `task-definition.backend.json` / `task-definition.frontend.json`: app containers (Spring Boot on 8080, Next.js on 3000). Both use `networkMode: bridge` with dynamic host ports (`hostPort: 0`) so dev/prod instances of the same service can coexist on one EC2 host.
- `task-definition.caddy.json`: a single shared reverse-proxy container (not built from this repo's Dockerfiles — pulls `caddy:2-alpine` directly) that terminates TLS/HTTP(S)/HTTP3 on ports 80/443 and Caddyfile-routes by domain: `dev.watchtower.boo` splits `/swagger-ui/*` and `/v3/api-docs*` to the dev backend and everything else to the dev frontend; `watchtower.boo` (prod) routes everything to the prod frontend only — the prod backend has no public route through Caddy.

## Where to look

- Change a container's CPU/memory/health check/log group: the matching `task-definition.*.json` file directly (these are static JSON, rendered and pushed by the deploy workflow via `aws-actions/amazon-ecs-render-task-definition`).
- Change Caddy routing rules or add a new upstream: the `command` array inside `task-definition.caddy.json`'s `containerDefinitions[0]` (the Caddyfile is embedded there, not a separate file).
- Change which env selects which upstream: the `environment` block in `task-definition.caddy.json` (`CADDY_DEV_SITE_ADDRESS`, `DEV_BACKEND_UPSTREAM`, `DEV_FRONTEND_UPSTREAM`, `CADDY_PROD_SITE_ADDRESS`, `PROD_FRONTEND_UPSTREAM`).

## Local conventions

- Task family/container names follow `watchtower-{service}-task` / `watchtower-{service}-container`; the deploy workflow derives ECS service names as `watchtower-{service}-{env}-task-service` (caddy is the one exception: `watchtower-caddy-task-service`, no env suffix, since it's shared).
- All three tasks share the same IAM role (`watchtower-ecs-role`) for both task and execution roles, and log to CloudWatch under `/ecs/{family}`.

## Local anti-patterns / gotchas

- Caddy's prod site block has no backend route — if the prod backend ever needs a public HTTP path (e.g. webhooks), it must be added explicitly to `task-definition.caddy.json`'s prod handle block, not assumed to work like dev.
- `caddy-data` is a bind-mounted host volume (`/var/lib/watchtower/caddy-data`) for ACME cert storage; it only exists on whichever EC2 instance the Caddy task lands on, so Caddy must be pinned to run on the same instance across deploys (not documented here — verify placement/instance affinity before changing Caddy's task definition).
