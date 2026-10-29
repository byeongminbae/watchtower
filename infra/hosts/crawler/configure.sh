#!/usr/bin/env bash
# 크롤러 앱의 환경변수를 관리자 PC 에서 Dokku 에 넣음. 비밀값은 저장소에 두지 않음
# 사용법: bash configure.sh <ssh 대상(ubuntu@IP)> <infra/oci/crawler.sh 가 만든 crawler.env> <크롤러 API 개인키 pem>
set -euo pipefail

TARGET=$1
ENV_FILE=$2
PRIVATE_KEY_FILE=$3
CONCURRENCY=${CRAWLER_CONCURRENCY:-2}

# shellcheck disable=SC1090
source "$ENV_FILE"

encoded() { # name value
  printf '%s=%s' "$1" "$(printf '%s' "$2" | base64 -w0)"
}

ssh "$TARGET" sudo dokku config:set --encoded --no-restart crawler \
  "$(encoded OCI_REGION "$OCI_REGION")" \
  "$(encoded OCI_TENANCY_ID "$OCI_TENANCY_ID")" \
  "$(encoded OCI_USER_ID "$OCI_USER_ID")" \
  "$(encoded OCI_FINGERPRINT "$OCI_FINGERPRINT")" \
  "$(encoded OCI_PRIVATE_KEY "$(cat "$PRIVATE_KEY_FILE")")" \
  "$(encoded OBJECT_STORAGE_NAMESPACE "$OBJECT_STORAGE_NAMESPACE")" \
  "$(encoded CRAWLER_CONCURRENCY "$CONCURRENCY")" \
  "$(encoded CRAWL_REQUEST_QUEUE_ID_DEV "$CRAWL_REQUEST_QUEUE_ID_DEV")" \
  "$(encoded CRAWL_REQUEST_QUEUE_ENDPOINT_DEV "$CRAWL_REQUEST_QUEUE_ENDPOINT_DEV")" \
  "$(encoded CRAWL_RESULT_QUEUE_ID_DEV "$CRAWL_RESULT_QUEUE_ID_DEV")" \
  "$(encoded CRAWL_RESULT_QUEUE_ENDPOINT_DEV "$CRAWL_RESULT_QUEUE_ENDPOINT_DEV")" \
  "$(encoded OBJECT_STORAGE_BUCKET_DEV watchtower-snapshot-dev)" \
  "$(encoded CRAWL_REQUEST_QUEUE_ID_PROD "$CRAWL_REQUEST_QUEUE_ID_PROD")" \
  "$(encoded CRAWL_REQUEST_QUEUE_ENDPOINT_PROD "$CRAWL_REQUEST_QUEUE_ENDPOINT_PROD")" \
  "$(encoded CRAWL_RESULT_QUEUE_ID_PROD "$CRAWL_RESULT_QUEUE_ID_PROD")" \
  "$(encoded CRAWL_RESULT_QUEUE_ENDPOINT_PROD "$CRAWL_RESULT_QUEUE_ENDPOINT_PROD")" \
  "$(encoded OBJECT_STORAGE_BUCKET_PROD watchtower-snapshot-prod)" >/dev/null
echo "crawler 환경변수 설정 완료. 다음 배포부터 반영됨"
