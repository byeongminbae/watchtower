#!/usr/bin/env bash
# 크롤러 컨테이너에서 Chromium 샌드박스를 띄울 수 있도록 Docker seccomp 를 끄고, 호스트의 AppArmor user namespace 제한을 끔. 다시 실행해도 결과가 같음
# 사용법: 크롤러 호스트에서 sudo bash crawler-sandbox.sh
# Chromium 샌드박스는 user namespace 를 만들어야 하는데, 두 곳이 이를 막음
#   1. Docker 기본 seccomp: user namespace 생성(clone, unshare) 시스템 콜을 막음
#   2. Ubuntu 23.10 이상의 AppArmor: kernel.apparmor_restrict_unprivileged_userns=1 이 기본값이라 일반 프로세스의 user namespace 생성을 막음
# 렌더러에는 Chromium 이 자체 seccomp 필터를 걸기 때문에, Docker seccomp 를 꺼도 렌더러는 계속 필터가 걸린 상태임
# AppArmor 제한은 호스트 전체 설정이라 다른 프로세스도 user namespace 를 만들 수 있게 됨. 크롤러 전용 호스트라 받아들이고, 컨테이너의 docker-default AppArmor 프로필은 그대로 유지함
# 적용하지 않으면 크롤러가 시작할 때 브라우저를 띄우지 못해 기동을 멈추고, 배포 헬스체크가 실패해 이전 크롤러가 계속 돎
set -euo pipefail

seccomp_option="--security-opt seccomp=unconfined"
if ! dokku docker-options:report crawler --docker-options-deploy | grep -qF "$seccomp_option"; then
  dokku docker-options:add crawler deploy "$seccomp_option"
fi

restrict_userns=/proc/sys/kernel/apparmor_restrict_unprivileged_userns
if [ -f "$restrict_userns" ]; then
  echo "kernel.apparmor_restrict_unprivileged_userns=0" > /etc/sysctl.d/60-crawler-chromium-userns.conf
  sysctl --quiet --load /etc/sysctl.d/60-crawler-chromium-userns.conf
fi

echo "seccomp 를 끄는 실행 옵션을 등록하고 AppArmor user namespace 제한을 끔. 크롤러를 다시 배포해야 반영됨"
