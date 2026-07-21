#!/usr/bin/env bash
set -euo pipefail # 하나라도 실패시 스크립트 종료(엄격)

# 예시:
# ./deploy.sh backend dev ghcr.io/owner/watchtower-backend-dev:abc123
# 사전 조건: EC2에서 이미 `docker login ghcr.io` 되어 있어야 함

SERVICE=${1:-}
DEPLOY_ENV=${2:-}
IMAGE_TAG=${3:-}

if [[ -z "$SERVICE" || -z "$DEPLOY_ENV" || -z "$IMAGE_TAG" ]]; then
  echo "사용법: $0 <backend|frontend> <dev|prod> <image_tag>"
  exit 1
fi

if [[ "$SERVICE" != "backend" && "$SERVICE" != "frontend" ]]; then
  echo "잘못된 service 인자: $SERVICE (backend|frontend 만 허용)"
  exit 1
fi

if [[ "$DEPLOY_ENV" != "dev" && "$DEPLOY_ENV" != "prod" ]]; then
  echo "잘못된 env 인자: $DEPLOY_ENV (dev|prod 만 허용)"
  exit 1
fi

CONTAINER_PREFIX="watchtower-${SERVICE}-${DEPLOY_ENV}"
V1_NAME="${CONTAINER_PREFIX}-v1"
V2_NAME="${CONTAINER_PREFIX}-v2"

PORT_V1=8080
PORT_V2=8081

NGINX_UPSTREAM_CONF="/etc/nginx/conf.d/${SERVICE}-${DEPLOY_ENV}-upstream.conf"
HEALTH_CHECK_PATH="/actuator/health"
HEALTH_CHECK_TIMEOUT_SEC=60
HEALTH_CHECK_INTERVAL_SEC=1

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

# 0. GHCR에서 이미지 pull
log "GHCR에서 이미지 pull 중: ${IMAGE_TAG}"
docker pull "${IMAGE_TAG}"

# 1. 컨테이너 상태 확인 헬퍼
is_container_running() {
  local name="$1"
  [[ "$(docker inspect -f '{{.State.Running}}' "${name}" 2>/dev/null || echo false)" == "true" ]]
}

# 2. 컨테이너 실행 함수
run_container() {
  local name="$1"
  local port="$2"

  log "컨테이너 실행: ${name} (host port ${port} -> container port 8080)"

  # 기존 동일 이름 컨테이너가 죽어있는 상태로 남아있으면 정리
  docker rm -f "${name}" >/dev/null 2>&1 || true

  docker run -d \
    --name "${name}" \
    --network watchtower-net \
    --restart unless-stopped \
    -p "${port}:8080" \
    --env-file "/home/$(whoami)/deploy/env/${SERVICE}.${DEPLOY_ENV}.env" \
    "${IMAGE_TAG}"
}


# 3. 헬스체크 함수
health_check() {
  local port="$1"
  local elapsed=0

  log "헬스체크 시작: http://localhost:${port}${HEALTH_CHECK_PATH} (최대 ${HEALTH_CHECK_TIMEOUT_SEC}초)"

  while (( elapsed < HEALTH_CHECK_TIMEOUT_SEC )); do
    if curl -fs -o /dev/null -m 3 "http://localhost:${port}${HEALTH_CHECK_PATH}"; then
      log "헬스체크 성공 (${elapsed}초 경과)"
      return 0
    fi
    sleep "${HEALTH_CHECK_INTERVAL_SEC}"
    elapsed=$(( elapsed + HEALTH_CHECK_INTERVAL_SEC ))
    log "헬스체크 대기 중... (${elapsed}초 경과)"
  done

  log "헬스체크 실패: ${HEALTH_CHECK_TIMEOUT_SEC}초 내 응답 없음"
  return 1
}

# upstream backend_dev_upstream {
#     include /etc/nginx/upstreams/backend_dev.conf;
# }
# upstream backend_prod_upstream {
#     include /etc/nginx/upstreams/backend_prod.conf;
# }
# upstream frontend_dev_upstream {
#     include /etc/nginx/upstreams/frontend_dev.conf;
# }
# upstream frontend_prod_upstream {
#     include /etc/nginx/upstreams/frontend_prod.conf;
# }

# 4. nginx 업스트림 전환 함수
switch_nginx_upstream() {
  local port="$1"
  local upstream_conf="/etc/nginx/upstreams/${SERVICE}_${DEPLOY_ENV}.conf"

  log "nginx 업스트림을 포트 ${port} 로 전환합니다."

  sudo mkdir -p /etc/nginx/upstreams

  cat | sudo tee "${;;sxss}" > /dev/null <<EOF
server 127.0.0.1:${port};
EOF

  sudo nginx -t
  sudo systemctl reload nginx

  log "nginx reload 완료 (업스트림 -> ${port})"
}


# 5. 메인 로직: v1/v2 상태 판단
V1_RUNNING=false
V2_RUNNING=false

is_container_running "${V1_NAME}" && V1_RUNNING=true
is_container_running "${V2_NAME}" && V2_RUNNING=true

if [[ "${V1_RUNNING}" == false && "${V2_RUNNING}" == false ]]; then
  echo "초기 실행 모드"

  run_container "${V1_NAME}" "${PORT_V1}"

  if health_check "${PORT_V1}"; then
    switch_nginx_upstream "${PORT_V1}"
    log "초기 실행 모드 배포 완료 (${V1_NAME}, port ${PORT_V1})"
    exit 0
  else
    log "초기 실행 모드 헬스체크 실패. 컨테이너 로그 확인 필요."
    docker logs --tail 100 "${V1_NAME}" || true
    docker rm -f "${V1_NAME}" >/dev/null 2>&1 || true
    exit 1
  fi

elif [[ "${V1_RUNNING}" == true && "${V2_RUNNING}" == true ]]; then
  echo "포트 전체 점유상태. 관리자 확인 필요."
  exit 1

else
  echo "무중단 배포 모드"

  if [[ "${V1_RUNNING}" == true ]]; then
    OLD_NAME="${V1_NAME}"
    OLD_PORT="${PORT_V1}"
    NEW_NAME="${V2_NAME}"
    NEW_PORT="${PORT_V2}"
  else
    OLD_NAME="${V2_NAME}"
    OLD_PORT="${PORT_V2}"
    NEW_NAME="${V1_NAME}"
    NEW_PORT="${PORT_V1}"
  fi

  log "기존 버전: ${OLD_NAME} (port ${OLD_PORT}) / 신규 버전: ${NEW_NAME} (port ${NEW_PORT})"

  run_container "${NEW_NAME}" "${NEW_PORT}"

  if health_check "${NEW_PORT}"; then
    switch_nginx_upstream "${NEW_PORT}"

    log "기존 컨테이너 종료: ${OLD_NAME}"
    docker stop "${OLD_NAME}" >/dev/null 2>&1 || true
    docker rm "${OLD_NAME}" >/dev/null 2>&1 || true

    log "무중단 배포 완료 (${NEW_NAME}, port ${NEW_PORT})"
    exit 0
  else
    log "신규 버전 헬스체크 실패. 배포를 중단하고 신규 컨테이너를 정리합니다."
    docker logs --tail 100 "${NEW_NAME}" || true
    docker stop "${NEW_NAME}" >/dev/null 2>&1 || true
    docker rm "${NEW_NAME}" >/dev/null 2>&1 || true
    log "기존 버전(${OLD_NAME})은 그대로 유지됩니다."
    exit 1
  fi
fi
