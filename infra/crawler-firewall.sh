#!/usr/bin/env bash
# 크롤러 호스트에서 컨테이너가 내부망과 인스턴스 메타데이터에 닿지 못하게 함. 다시 실행해도 결과가 같음
# 사용법: 크롤러 호스트에서 sudo bash crawler-firewall.sh
# 배포 workflow 가 매 배포마다 이 차단이 동작하는지 검사하고, 동작하지 않으면 크롤러를 멈춤
set -euo pipefail

# 컨테이너 DNS 를 공개 DNS 로 바꿈. 기본값인 OCI DNS(169.254.169.254)는 아래 규칙에 막혀 크롤링이 전부 실패함
daemon_config=/etc/docker/daemon.json
current_config=$(cat "$daemon_config" 2>/dev/null || echo '{}')
desired_config=$(jq '.dns = ["1.1.1.1", "8.8.8.8"]' <<<"$current_config")
if [ "$current_config" != "$desired_config" ]; then
  echo "$desired_config" > "$daemon_config"
  systemctl restart docker
fi

# 호스트 자체의 통신은 FORWARD 를 지나지 않으므로 호스트의 DNS 조회와 패키지 설치는 영향받지 않음
cat > /usr/local/sbin/docker-user-firewall <<'SCRIPT'
#!/usr/bin/env bash
set -euo pipefail
iptables -N DOCKER-USER 2>/dev/null || true
iptables -F DOCKER-USER
iptables -A DOCKER-USER -m conntrack --ctstate RELATED,ESTABLISHED -j RETURN
for cidr in 0.0.0.0/8 10.0.0.0/8 100.64.0.0/10 127.0.0.0/8 169.254.0.0/16 172.16.0.0/12 192.168.0.0/16 224.0.0.0/4; do
  iptables -A DOCKER-USER -d "$cidr" -j REJECT --reject-with icmp-net-prohibited
done
iptables -A DOCKER-USER -j RETURN
SCRIPT
chmod 755 /usr/local/sbin/docker-user-firewall

# Docker 가 재시작되거나 서버가 재부팅되어도 규칙을 다시 넣음
cat > /etc/systemd/system/docker-user-firewall.service <<'UNIT'
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
UNIT
systemctl daemon-reload
systemctl enable docker-user-firewall.service
systemctl restart docker-user-firewall.service
