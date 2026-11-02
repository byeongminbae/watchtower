#!/usr/bin/env bash
# 크롤러 컨테이너에서 Chromium 샌드박스를 띄울 수 있도록 Docker seccomp 를 끔. 다시 실행해도 결과가 같음
# 사용법: 크롤러 호스트에서 sudo bash crawler-sandbox.sh
# Docker 기본 seccomp 는 Chromium 샌드박스가 쓰는 user namespace 생성(clone, unshare)을 막음
# 렌더러에는 Chromium 이 자체 seccomp 필터를 걸기 때문에, Docker seccomp 를 꺼도 렌더러는 계속 필터가 걸린 상태임
# 적용하지 않으면 크롤러가 시작할 때 브라우저를 띄우지 못해 기동을 멈추고, 배포 헬스체크가 실패해 이전 크롤러가 계속 돎
set -euo pipefail

seccomp_option="--security-opt seccomp=unconfined"
if ! dokku docker-options:report crawler --docker-options-deploy | grep -qF "$seccomp_option"; then
  dokku docker-options:add crawler deploy "$seccomp_option"
fi

# Ubuntu 23.10 이상은 AppArmor 가 user namespace 생성을 따로 막을 수 있음. 이 경우 seccomp 를 꺼도 샌드박스가 뜨지 않음
restrict_userns=/proc/sys/kernel/apparmor_restrict_unprivileged_userns
if [ -f "$restrict_userns" ] && [ "$(cat "$restrict_userns")" = "1" ]; then
  echo "AppArmor 가 user namespace 생성을 막고 있음. 크롤러를 배포해 기동하는지 확인해야 함" >&2
fi

echo "seccomp 를 끄는 실행 옵션을 등록함. 크롤러를 다시 배포해야 반영됨"
