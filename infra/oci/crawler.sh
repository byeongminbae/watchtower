#!/usr/bin/env bash
# 크롤러 인스턴스와 전용 VCN, 큐, 스냅샷 버킷, IAM 을 만듦. 이미 있는 리소스는 건너뛰므로 다시 실행해도 됨
# 사용법: ADMIN_CIDR=x.x.x.x/32 OCI_USER_EMAIL=... SSH_PUBLIC_KEY_FILE=~/.ssh/xxx.pub DEPLOY_PUBLIC_KEY_FILE=... bash infra/oci/crawler.sh
# 끝나면 $OUT_DIR/crawler.env 와 크롤러 API 개인키로 infra/hosts/crawler/configure.sh 를 실행해 앱 환경변수를 넣음
#
# 크롤러는 사용자가 입력한 URL 을 브라우저로 열기 때문에 기존 VCN 과 연결하지 않은 별도 VCN 에 둠
set -euo pipefail
source "$(dirname "$0")/lib.sh"
: "${DEPLOY_PUBLIC_KEY_FILE:?GitHub Actions 배포용 SSH 공개키 경로를 DEPLOY_PUBLIC_KEY_FILE 로 지정해야 함}"

CRAWLER_PRIVATE_IP=10.1.0.187
DEPLOY_COMP=$(deploy_compartment)
: "${DEPLOY_COMP:?watchtower-deploy compartment 가 없음. infra/oci/core.sh 를 먼저 실행해야 함}"

echo "== VCN, 서브넷"
VCN=$(oci network vcn list -c $T --display-name watchtower-crawler-vcn | first)
if [ -z "$VCN" ]; then
  VCN=$(oci network vcn create -c $T --display-name watchtower-crawler-vcn --cidr-blocks '["10.1.0.0/16"]' \
    --dns-label crawlervcn --wait-for-state AVAILABLE --query data.id --raw-output)
fi
VCN_JSON=$(oci network vcn get --vcn-id "$VCN")
ROUTE_TABLE=$(jq -r '.data."default-route-table-id"' <<<"$VCN_JSON")
SECURITY_LIST=$(jq -r '.data."default-security-list-id"' <<<"$VCN_JSON")
IGW=$(oci network internet-gateway list -c $T --vcn-id "$VCN" | first)
if [ -z "$IGW" ]; then
  IGW=$(oci network internet-gateway create -c $T --vcn-id "$VCN" --display-name watchtower-crawler-igw \
    --is-enabled true --wait-for-state AVAILABLE --query data.id --raw-output)
fi
oci network route-table update --rt-id "$ROUTE_TABLE" --force \
  --route-rules "[{\"destination\":\"0.0.0.0/0\",\"destinationType\":\"CIDR_BLOCK\",\"networkEntityId\":\"$IGW\"}]" >/dev/null
oci network security-list update --security-list-id "$SECURITY_LIST" --force \
  --ingress-security-rules '[
    {"protocol":"1","source":"0.0.0.0/0","sourceType":"CIDR_BLOCK","icmpOptions":{"type":3,"code":4}},
    {"protocol":"1","source":"10.1.0.0/16","sourceType":"CIDR_BLOCK","icmpOptions":{"type":3}}]' >/dev/null
SUBNET=$(oci network subnet list -c $T --vcn-id "$VCN" --display-name watchtower-crawler-subnet | first)
if [ -z "$SUBNET" ]; then
  SUBNET=$(oci network subnet create -c $T --vcn-id "$VCN" --display-name watchtower-crawler-subnet \
    --cidr-block 10.1.0.0/24 --dns-label crawler --route-table-id "$ROUTE_TABLE" \
    --security-list-ids "[\"$SECURITY_LIST\"]" --wait-for-state AVAILABLE --query data.id --raw-output)
fi

echo "== NSG"
CRAWLER_NSG=$(ensure_nsg $T "$VCN" watchtower-crawler-nsg)
DEPLOY_NSG=$(ensure_nsg "$DEPLOY_COMP" "$VCN" watchtower-crawler-deploy-ssh-nsg)
ensure_nsg_rules "$CRAWLER_NSG" "[$(tcp_rule INGRESS "$ADMIN_CIDR" CIDR_BLOCK 22 'admin ssh')]"

echo "== 큐"
ensure_queue() { # name
  local id
  id=$(oci queue queue-admin queue list -c $T --all --display-name "$1" --lifecycle-state ACTIVE | first_item)
  if [ -z "$id" ]; then
    oci queue queue-admin queue create -c $T --display-name "$1" --retention-in-seconds 86400 \
      --visibility-in-seconds 120 --timeout-in-seconds 30 --dlq-delivery-count 5 --wait-for-state SUCCEEDED >/dev/null
    id=$(oci queue queue-admin queue list -c $T --all --display-name "$1" --lifecycle-state ACTIVE | first_item)
  fi
  echo "$id"
}
REQUEST_DEV=$(ensure_queue watchtower-crawl-request-dev)
REQUEST_PROD=$(ensure_queue watchtower-crawl-request-prod)
RESULT_DEV=$(ensure_queue watchtower-crawl-result-dev)
RESULT_PROD=$(ensure_queue watchtower-crawl-result-prod)

echo "== IAM Policy"
BUCKETS="target.bucket.name = 'watchtower-snapshot-dev', target.bucket.name = 'watchtower-snapshot-prod'"
ensure_policy watchtower-objectstorage-lifecycle "Object Storage lifecycle deletes expired snapshots" \
  "[\"Allow service objectstorage-$REGION to manage object-family in tenancy where any {$BUCKETS}\"]"
CRAWLER_USER=$(ensure_api_user watchtower-crawler watchtower-crawler "watchtower crawler queue/object access")
ensure_policy watchtower-crawler-access "crawler consumes request queues, produces result queues, writes snapshots" "[
  \"Allow group watchtower-crawler to use queues in tenancy where all {request.permission = 'QUEUE_CONSUME', any {target.queue.id = '$REQUEST_DEV', target.queue.id = '$REQUEST_PROD'}}\",
  \"Allow group watchtower-crawler to use queues in tenancy where all {request.permission = 'QUEUE_PRODUCE', any {target.queue.id = '$RESULT_DEV', target.queue.id = '$RESULT_PROD'}}\",
  \"Allow group watchtower-crawler to manage objects in tenancy where all {any {$BUCKETS}, any {request.permission = 'OBJECT_CREATE', request.permission = 'OBJECT_OVERWRITE'}}\"]"
ensure_policy watchtower-app-crawl-access "app instance produces crawl requests, consumes results, reads snapshots" "[
  \"Allow dynamic-group watchtower-app-instance to use queues in tenancy where all {request.permission = 'QUEUE_PRODUCE', any {target.queue.id = '$REQUEST_DEV', target.queue.id = '$REQUEST_PROD'}}\",
  \"Allow dynamic-group watchtower-app-instance to use queues in tenancy where all {request.permission = 'QUEUE_CONSUME', any {target.queue.id = '$RESULT_DEV', target.queue.id = '$RESULT_PROD'}}\",
  \"Allow dynamic-group watchtower-app-instance to read objects in tenancy where any {$BUCKETS}\"]"
ensure_api_key "$CRAWLER_USER" crawler-api-key

echo "== 스냅샷 버킷 (31일 지나면 자동 삭제)"
NAMESPACE=$(oci os ns get --query data --raw-output)
for env in dev prod; do
  bucket=watchtower-snapshot-$env
  if ! oci os bucket get --bucket-name "$bucket" >/dev/null 2>&1; then
    oci os bucket create -c $T --name "$bucket" --public-access-type NoPublicAccess --storage-tier Standard >/dev/null
  fi
  # 수명 주기 Policy 가 전파되기 전에는 실패할 수 있어 재시도함
  for _ in $(seq 1 12); do
    oci os object-lifecycle-policy put --bucket-name "$bucket" --force --items '[
      {"name":"expire-snapshots","action":"DELETE","timeAmount":31,"timeUnit":"DAYS","isEnabled":true,"target":"objects"}]' \
      >/dev/null 2>&1 && break
    sleep 10
  done
done

echo "== 크롤러 인스턴스 (cloud-init 으로 서버 설정 스크립트 실행)"
USER_DATA=$(user_data_file "$INFRA_DIR/hosts/crawler/provision.sh" "DEPLOY_PUBLIC_KEY=$(cat "$DEPLOY_PUBLIC_KEY_FILE")")
INSTANCE=$(ensure_instance watchtower-crawler-instance "$SUBNET" $CRAWLER_PRIVATE_IP true "[\"$CRAWLER_NSG\",\"$DEPLOY_NSG\"]" "$USER_DATA")
rm -f "$USER_DATA"
PUBLIC_IP=$(oci compute instance list-vnics --instance-id "$INSTANCE" --query 'data[0]."public-ip"' --raw-output)

echo "== GitHub variables"
gh variable set CRAWLER_DOKKU_HOST -R $REPO -b "$PUBLIC_IP"
gh variable set OCI_CRAWLER_DEPLOY_NSG_ID -R $REPO -b "$DEPLOY_NSG"

messages_endpoint() { oci queue queue-admin queue get --queue-id "$1" --query 'data."messages-endpoint"' --raw-output; }
cat > "$OUT_DIR/crawler.env" <<EOF
CRAWLER_PUBLIC_IP=$PUBLIC_IP
OCI_REGION=$REGION
OCI_TENANCY_ID=$T
OCI_USER_ID=$CRAWLER_USER
OCI_FINGERPRINT=$(cat "$OUT_DIR/crawler-api-key.fingerprint")
OBJECT_STORAGE_NAMESPACE=$NAMESPACE
CRAWL_REQUEST_QUEUE_ID_DEV=$REQUEST_DEV
CRAWL_REQUEST_QUEUE_ID_PROD=$REQUEST_PROD
CRAWL_RESULT_QUEUE_ID_DEV=$RESULT_DEV
CRAWL_RESULT_QUEUE_ID_PROD=$RESULT_PROD
CRAWL_REQUEST_QUEUE_ENDPOINT_DEV=$(messages_endpoint "$REQUEST_DEV")
CRAWL_REQUEST_QUEUE_ENDPOINT_PROD=$(messages_endpoint "$REQUEST_PROD")
CRAWL_RESULT_QUEUE_ENDPOINT_DEV=$(messages_endpoint "$RESULT_DEV")
CRAWL_RESULT_QUEUE_ENDPOINT_PROD=$(messages_endpoint "$RESULT_PROD")
EOF

cat <<EOF
완료. 남은 수동 작업:
- CRAWLER_DOKKU_KNOWN_HOSTS 변수(크롤러 서버 호스트 키) 등록
- bash infra/hosts/crawler/configure.sh <ssh 대상> $OUT_DIR/crawler.env $OUT_DIR/crawler-api-key.pem
- GitHub Actions 에서 crawler 배포
EOF
