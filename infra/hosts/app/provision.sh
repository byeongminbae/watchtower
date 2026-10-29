#!/usr/bin/env bash
# 앱 인스턴스(백엔드, 프론트의 dev/prod)를 구성함. cloud-init 으로 첫 부팅 때 실행되고, 손으로 다시 실행해도 결과가 같음
# 사용법: sudo LETSENCRYPT_EMAIL=... DEPLOY_PUBLIC_KEY="ssh-ed25519 ..." bash provision.sh
#
# 배포 순서 주의: front-dev 의 swagger 프록시가 back-dev 의 nginx upstream 을 참조하므로 back-dev 를 먼저 배포해야 함
# 인증서는 DNS 가 이 서버를 가리키고 프론트를 배포한 뒤 dokku letsencrypt:enable front-dev front-prod 로 발급함
set -euo pipefail

DOKKU_VERSION=v0.38.32
LETSENCRYPT_PLUGIN_VERSION=0.25.2

if ! command -v dokku >/dev/null; then
  wget -q -O /tmp/dokku-bootstrap.sh "https://dokku.com/install/$DOKKU_VERSION/bootstrap.sh"
  DEBIAN_FRONTEND=noninteractive DOKKU_TAG=$DOKKU_VERSION bash /tmp/dokku-bootstrap.sh
fi
dokku plugin:installed letsencrypt || dokku plugin:install https://github.com/dokku/dokku-letsencrypt.git --committish $LETSENCRYPT_PLUGIN_VERSION

# OCI 이미지의 기본 방화벽은 SSH 외의 접속을 모두 거부함
for port in 80 443; do
  iptables -C INPUT -p tcp --dport $port -m state --state NEW -j ACCEPT 2>/dev/null \
    || iptables -I INPUT 5 -p tcp --dport $port -m state --state NEW -j ACCEPT
done
iptables-save > /etc/iptables/rules.v4

# IP 나 등록되지 않은 도메인으로 온 요청은 어떤 앱에도 넘기지 않음
cat > /etc/nginx/conf.d/00-default-vhost.conf <<'EOF'
server {
  listen 80 default_server;
  listen [::]:80 default_server;
  listen 443 ssl default_server;
  listen [::]:443 ssl default_server;
  server_name _;
  ssl_reject_handshake on;
  access_log off;
  return 444;
}
EOF
nginx -t && systemctl reload nginx

dokku domains:clear-global >/dev/null
for env in dev prod; do
  dokku network:exists watchtower-$env 2>/dev/null || dokku network:create watchtower-$env
  for app in back-$env front-$env; do
    dokku apps:exists $app 2>/dev/null || dokku apps:create $app
    dokku network:set $app attach-post-create watchtower-$env >/dev/null
  done
  dokku resource:limit --memory 1024m back-$env >/dev/null
  dokku resource:limit --memory 512m front-$env >/dev/null
  dokku ports:report front-$env --ports-map | grep -q "http:80:3000" || dokku ports:add front-$env http:80:3000
done
dokku domains:set front-dev dev.watchtower.boo >/dev/null
dokku domains:set front-prod watchtower.boo >/dev/null

# back-prod 는 프론트만 내부 네트워크로 호출함
dokku proxy:disable back-prod >/dev/null
# back-dev 는 front-dev 의 swagger 경로용 nginx upstream 만 필요함. 80 으로 두면 IP 로 들어온 요청이 dev API 로 감
dokku domains:clear back-dev >/dev/null
dokku ports:set back-dev http:18080:8080 >/dev/null

install -d -o dokku -g dokku /home/dokku/front-dev/nginx.conf.d
cat > /home/dokku/front-dev/nginx.conf.d/backend-docs.conf <<'EOF'
location /swagger-ui/ {
  proxy_pass http://back-dev-8080;
  proxy_set_header Host $http_host;
  proxy_set_header X-Forwarded-For $remote_addr;
  proxy_set_header X-Forwarded-Proto $scheme;
}

location /v3/api-docs {
  proxy_pass http://back-dev-8080;
  proxy_set_header Host $http_host;
  proxy_set_header X-Forwarded-For $remote_addr;
  proxy_set_header X-Forwarded-Proto $scheme;
}
EOF
chown dokku:dokku /home/dokku/front-dev/nginx.conf.d/backend-docs.conf

dokku letsencrypt:set --global email "$LETSENCRYPT_EMAIL" >/dev/null
dokku letsencrypt:cron-job --add >/dev/null

if [ -n "${DEPLOY_PUBLIC_KEY:-}" ] && ! dokku ssh-keys:list 2>/dev/null | grep -q 'NAME="github-actions"'; then
  echo "$DEPLOY_PUBLIC_KEY" | dokku ssh-keys:add github-actions
fi
