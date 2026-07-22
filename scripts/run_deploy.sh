#!/usr/bin/env bash
set -euo pipefail

source "../deploy.env"

exec ./deploy.sh \
    "$SERVICE" \
    "$DEPLOY_ENV" \
    "$IMAGE_TAG"