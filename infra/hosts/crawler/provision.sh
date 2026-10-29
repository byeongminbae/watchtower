#!/usr/bin/env bash
# 크롤러 인스턴스를 구성함. cloud-init 으로 첫 부팅 때 실행되고, 손으로 다시 실행해도 결과가 같음
# 사용법: sudo DEPLOY_PUBLIC_KEY="ssh-ed25519 ..." bash provision.sh
set -euo pipefail

DOKKU_VERSION=v0.38.32
APP=crawler

command -v jq >/dev/null || DEBIAN_FRONTEND=noninteractive apt-get install -y -qq jq

if ! command -v dokku >/dev/null; then
  wget -q -O /tmp/dokku-bootstrap.sh "https://dokku.com/install/$DOKKU_VERSION/bootstrap.sh"
  DEBIAN_FRONTEND=noninteractive DOKKU_TAG=$DOKKU_VERSION bash /tmp/dokku-bootstrap.sh
fi

# 컨테이너가 쓰는 DNS 를 공개 DNS 로 바꿈. 기본값인 OCI DNS(169.254.169.254)는 아래 규칙으로 막히고, VCN 내부 이름을 알려줄 이유도 없음
daemon_config=/etc/docker/daemon.json
current_config=$(cat "$daemon_config" 2>/dev/null || echo '{}')
desired_config=$(jq '.dns = ["1.1.1.1", "8.8.8.8"]' <<<"$current_config")
if [ "$current_config" != "$desired_config" ]; then
  echo "$desired_config" > "$daemon_config"
  systemctl restart docker
fi

# 크롤러 컨테이너가 내부망과 인스턴스 메타데이터에 닿지 못하게 함
# 호스트 자체의 통신은 FORWARD 를 지나지 않으므로 OCI DNS(169.254.169.254) 조회는 영향받지 않음
cat > /usr/local/sbin/docker-user-firewall <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
iptables -N DOCKER-USER 2>/dev/null || true
iptables -F DOCKER-USER
iptables -A DOCKER-USER -m conntrack --ctstate RELATED,ESTABLISHED -j RETURN
for cidr in 0.0.0.0/8 10.0.0.0/8 100.64.0.0/10 127.0.0.0/8 169.254.0.0/16 172.16.0.0/12 192.168.0.0/16 224.0.0.0/4; do
  iptables -A DOCKER-USER -d "$cidr" -j REJECT --reject-with icmp-net-prohibited
done
iptables -A DOCKER-USER -j RETURN
EOF
chmod 755 /usr/local/sbin/docker-user-firewall

cat > /etc/systemd/system/docker-user-firewall.service <<'EOF'
[Unit]
Description=Block container egress to internal networks and instance metadata
After=docker.service
BindsTo=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
ExecStart=/usr/local/sbin/docker-user-firewall

[Install]
WantedBy=docker.service
EOF
systemctl daemon-reload
systemctl enable docker-user-firewall.service
systemctl restart docker-user-firewall.service

dokku apps:exists $APP 2>/dev/null || dokku apps:create $APP
dokku proxy:disable $APP
dokku resource:limit --memory 4608m $APP >/dev/null
# Chromium 은 기본 공유 메모리(64MB)로는 큰 페이지에서 죽고, PID 1 이 node 면 좀비 프로세스를 거두지 못함
for option in "--shm-size=1g" "--init"; do
  dokku docker-options:report $APP --docker-options-deploy | grep -q -- "$option" || dokku docker-options:add $APP deploy "$option"
done
# 처리 중인 크롤링과 모아둔 결과 발행을 끝낼 시간(폴링 30초 + 크롤링 30초 + 여유)
dokku config:set --no-restart $APP DOKKU_DOCKER_STOP_TIMEOUT=90 >/dev/null

if [ -n "${DEPLOY_PUBLIC_KEY:-}" ] && ! dokku ssh-keys:list 2>/dev/null | grep -q 'NAME="github-actions"'; then
  echo "$DEPLOY_PUBLIC_KEY" | dokku ssh-keys:add github-actions
fi
