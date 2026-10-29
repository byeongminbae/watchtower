#!/usr/bin/env bash
# DB 인스턴스를 구성함. cloud-init 으로 첫 부팅 때 실행되고, 손으로 다시 실행해도 결과가 같음
# 사용법: sudo APP_PRIVATE_IP=10.0.0.23 ADMIN_CIDR=x.x.x.x/32 [DEV_DB_PASSWORD=... PROD_DB_PASSWORD=...] bash provision.sh
#
# 데이터는 재현 대상이 아님. 비밀번호를 주면 환경별 계정과 빈 DB 를 만들고, 스키마는 백엔드 배포 시 Flyway 가 만듦
# 비밀번호는 각 환경 Vault 의 spring.datasource.password 와 같아야 함
set -euo pipefail

PG_VERSION=18
PG_CONF_DIR=/etc/postgresql/$PG_VERSION/main
PRIVATE_IP=$(hostname -I | awk '{print $1}')

if ! command -v psql >/dev/null; then
  apt-get update -qq
  DEBIAN_FRONTEND=noninteractive apt-get install -y -qq postgresql-$PG_VERSION
fi

config_changed=false
listen_line="listen_addresses = 'localhost,$PRIVATE_IP'"
if ! grep -qxF "$listen_line" $PG_CONF_DIR/postgresql.conf; then
  sed -i "s/^#\?listen_addresses.*/$listen_line/" $PG_CONF_DIR/postgresql.conf
  config_changed=true
fi

add_hba() {
  if ! grep -qxF "$1" $PG_CONF_DIR/pg_hba.conf; then
    echo "$1" >> $PG_CONF_DIR/pg_hba.conf
    config_changed=true
  fi
}
add_hba "host watchtower_dev watchtower_dev $APP_PRIVATE_IP/32 scram-sha-256"
add_hba "host watchtower_prod watchtower_prod $APP_PRIVATE_IP/32 scram-sha-256"
add_hba "host all all $ADMIN_CIDR scram-sha-256"

# OCI 이미지의 기본 방화벽은 SSH 외의 접속을 모두 거부함
for source in "$APP_PRIVATE_IP/32" "$ADMIN_CIDR"; do
  iptables -C INPUT -p tcp -s "$source" --dport 5432 -m state --state NEW -j ACCEPT 2>/dev/null \
    || iptables -I INPUT 5 -p tcp -s "$source" --dport 5432 -m state --state NEW -j ACCEPT
done
iptables-save > /etc/iptables/rules.v4

# 운영 중에 다시 실행해도 접속이 끊기지 않도록 설정이 바뀐 경우에만 재시작함
if [ "$config_changed" = true ]; then
  systemctl restart postgresql@$PG_VERSION-main
fi

create_environment() { # env password
  [ -n "$2" ] || return 0
  sudo -u postgres psql -q -v ON_ERROR_STOP=1 -v env="$1" -v password="$2" <<'EOF'
SELECT format('CREATE ROLE watchtower_%s LOGIN PASSWORD %L', :'env', :'password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'watchtower_' || :'env') \gexec
SELECT format('CREATE DATABASE watchtower_%s OWNER watchtower_%s', :'env', :'env')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'watchtower_' || :'env') \gexec
SELECT format('REVOKE ALL ON DATABASE watchtower_%s FROM PUBLIC', :'env') \gexec
EOF
}
create_environment dev "${DEV_DB_PASSWORD:-}"
create_environment prod "${PROD_DB_PASSWORD:-}"
