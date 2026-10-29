#!/usr/bin/env bash
# 앱, DB 인스턴스와 그 네트워크, Vault, IAM, GitHub 배포 설정을 만듦. 이미 있는 리소스는 건너뛰므로 다시 실행해도 됨
# 사용법: ADMIN_CIDR=x.x.x.x/32 OCI_USER_EMAIL=... SSH_PUBLIC_KEY_FILE=~/.ssh/xxx.pub \
#         DEPLOY_PUBLIC_KEY_FILE=... LETSENCRYPT_EMAIL=... bash infra/oci/core.sh
#
# 재현하지 않는 것: Vault secret 값, DB 데이터, DNS 레코드, TLS 인증서
set -euo pipefail
source "$(dirname "$0")/lib.sh"
: "${DEPLOY_PUBLIC_KEY_FILE:?GitHub Actions 배포용 SSH 공개키 경로를 DEPLOY_PUBLIC_KEY_FILE 로 지정해야 함}"
: "${LETSENCRYPT_EMAIL:?인증서 발급용 이메일을 LETSENCRYPT_EMAIL 로 지정해야 함}"

APP_PRIVATE_IP=10.0.0.23
DB_PRIVATE_IP=10.0.0.163

echo "== 배포용 compartment"
DEPLOY_COMP=$(deploy_compartment)
if [ -z "$DEPLOY_COMP" ]; then
  DEPLOY_COMP=$(oci iam compartment create -c $T --name watchtower-deploy --description "GitHub Actions deploy SSH NSG" \
    --wait-for-state ACTIVE --query data.id --raw-output)
fi

echo "== VCN, 서브넷"
VCN=$(oci network vcn list -c $T --display-name watchtower-vcn | first)
if [ -z "$VCN" ]; then
  VCN=$(oci network vcn create -c $T --display-name watchtower-vcn --cidr-blocks '["10.0.0.0/16"]' \
    --dns-label vcn10282357 --wait-for-state AVAILABLE --query data.id --raw-output)
fi
VCN_JSON=$(oci network vcn get --vcn-id "$VCN")
ROUTE_TABLE=$(jq -r '.data."default-route-table-id"' <<<"$VCN_JSON")
SECURITY_LIST=$(jq -r '.data."default-security-list-id"' <<<"$VCN_JSON")
IGW=$(oci network internet-gateway list -c $T --vcn-id "$VCN" | first)
if [ -z "$IGW" ]; then
  IGW=$(oci network internet-gateway create -c $T --vcn-id "$VCN" --display-name "Internet Gateway watchtower-vcn" \
    --is-enabled true --wait-for-state AVAILABLE --query data.id --raw-output)
fi
oci network route-table update --rt-id "$ROUTE_TABLE" --force \
  --route-rules "[{\"destination\":\"0.0.0.0/0\",\"destinationType\":\"CIDR_BLOCK\",\"networkEntityId\":\"$IGW\"}]" >/dev/null
# SSH 는 NSG 로만 허용함. 서브넷 전체에 열린 규칙을 남기지 않음
oci network security-list update --security-list-id "$SECURITY_LIST" --force \
  --ingress-security-rules '[
    {"protocol":"1","source":"0.0.0.0/0","sourceType":"CIDR_BLOCK","icmpOptions":{"type":3,"code":4}},
    {"protocol":"1","source":"10.0.0.0/16","sourceType":"CIDR_BLOCK","icmpOptions":{"type":3}}]' >/dev/null
SUBNET=$(oci network subnet list -c $T --vcn-id "$VCN" --display-name watchtower-subnet | first)
if [ -z "$SUBNET" ]; then
  SUBNET=$(oci network subnet create -c $T --vcn-id "$VCN" --display-name watchtower-subnet --cidr-block 10.0.0.0/24 \
    --dns-label subnet10282357 --route-table-id "$ROUTE_TABLE" --security-list-ids "[\"$SECURITY_LIST\"]" \
    --wait-for-state AVAILABLE --query data.id --raw-output)
fi

echo "== NSG"
APP_NSG=$(ensure_nsg $T "$VCN" watchtower-app-nsg)
DB_NSG=$(ensure_nsg $T "$VCN" watchtower-db-nsg)
DEPLOY_NSG=$(ensure_nsg "$DEPLOY_COMP" "$VCN" watchtower-deploy-ssh-nsg)
ensure_nsg_rules "$APP_NSG" "[
  $(tcp_rule INGRESS 0.0.0.0/0 CIDR_BLOCK 80 http),
  $(tcp_rule INGRESS 0.0.0.0/0 CIDR_BLOCK 443 https),
  $(tcp_rule INGRESS "$ADMIN_CIDR" CIDR_BLOCK 22 'admin ssh')]"
ensure_nsg_rules "$DB_NSG" "[
  $(tcp_rule INGRESS "$APP_NSG" NETWORK_SECURITY_GROUP 5432 'postgres from app'),
  $(tcp_rule INGRESS "$ADMIN_CIDR" CIDR_BLOCK 5432 'admin postgres'),
  $(tcp_rule INGRESS "$ADMIN_CIDR" CIDR_BLOCK 22 'admin ssh')]"

echo "== Vault (secret 값은 직접 넣어야 함)"
for env in local dev prod; do
  vault=$(oci kms management vault list -c $T --all --query "data[?\"display-name\"=='watchtower-$env-vault' && \"lifecycle-state\"=='ACTIVE'] | [0].id" --raw-output 2>/dev/null || true)
  if [ -z "$vault" ] || [ "$vault" = "null" ]; then
    vault=$(oci kms management vault create -c $T --display-name watchtower-$env-vault --vault-type DEFAULT \
      --wait-for-state ACTIVE --query data.id --raw-output)
  fi
  endpoint=$(oci kms management vault get --vault-id "$vault" --query 'data."management-endpoint"' --raw-output)
  if [ "$(oci kms management key list -c $T --endpoint "$endpoint" --all | count)" = "0" ]; then
    oci kms management key create -c $T --endpoint "$endpoint" --display-name watchtower-$env-key \
      --key-shape '{"algorithm":"AES","length":32}' --protection-mode SOFTWARE --wait-for-state ENABLED >/dev/null
  fi
  echo "$env vault: $vault"
done

echo "== 인스턴스 (cloud-init 으로 서버 설정 스크립트 실행)"
DEPLOY_PUBLIC_KEY=$(cat "$DEPLOY_PUBLIC_KEY_FILE")
APP_USER_DATA=$(user_data_file "$INFRA_DIR/hosts/app/provision.sh" "LETSENCRYPT_EMAIL=$LETSENCRYPT_EMAIL" "DEPLOY_PUBLIC_KEY=$DEPLOY_PUBLIC_KEY")
DB_USER_DATA=$(user_data_file "$INFRA_DIR/hosts/db/provision.sh" "APP_PRIVATE_IP=$APP_PRIVATE_IP" "ADMIN_CIDR=$ADMIN_CIDR")
APP_INSTANCE=$(ensure_instance watchtower-app-instance "$SUBNET" $APP_PRIVATE_IP false "[\"$APP_NSG\",\"$DEPLOY_NSG\"]" "$APP_USER_DATA")
DB_INSTANCE=$(ensure_instance watchtower-db-instance "$SUBNET" $DB_PRIVATE_IP true "[\"$DB_NSG\"]" "$DB_USER_DATA")
rm -f "$APP_USER_DATA" "$DB_USER_DATA"

echo "== 앱 인스턴스 예약 공인 IP"
RESERVED_IP=$(oci network public-ip list -c $T --scope REGION --lifetime RESERVED --all \
  --query "data[?\"display-name\"=='watchtower-application-publicip'] | [0].id" --raw-output 2>/dev/null || true)
if [ -z "$RESERVED_IP" ] || [ "$RESERVED_IP" = "null" ]; then
  RESERVED_IP=$(oci network public-ip create -c $T --lifetime RESERVED --display-name watchtower-application-publicip \
    --query data.id --raw-output)
fi
APP_VNIC=$(oci compute instance list-vnics --instance-id "$APP_INSTANCE" | first)
APP_PRIVATE_IP_ID=$(oci network private-ip list --vnic-id "$APP_VNIC" | first)
if [ "$(oci network public-ip get --public-ip-id "$RESERVED_IP" --query 'data."assigned-entity-id"' --raw-output)" != "$APP_PRIVATE_IP_ID" ]; then
  oci network public-ip update --public-ip-id "$RESERVED_IP" --private-ip-id "$APP_PRIVATE_IP_ID" --wait-for-state ASSIGNED >/dev/null
fi
APP_PUBLIC_IP=$(oci network public-ip get --public-ip-id "$RESERVED_IP" --query 'data."ip-address"' --raw-output)

echo "== 앱 인스턴스 Vault 읽기 권한 (Instance Principal)"
if [ -z "$(oci iam dynamic-group list --name watchtower-app-instance | first)" ]; then
  oci iam dynamic-group create --name watchtower-app-instance --description "watchtower app instance" \
    --matching-rule "Any {instance.id = '$APP_INSTANCE'}" --wait-for-state ACTIVE >/dev/null
fi
DEV_VAULT=$(oci kms management vault list -c $T --all --query "data[?\"display-name\"=='watchtower-dev-vault' && \"lifecycle-state\"=='ACTIVE'] | [0].id" --raw-output)
PROD_VAULT=$(oci kms management vault list -c $T --all --query "data[?\"display-name\"=='watchtower-prod-vault' && \"lifecycle-state\"=='ACTIVE'] | [0].id" --raw-output)
ensure_policy watchtower-app-vault-read "app instance reads dev/prod vault" \
  "[\"Allow dynamic-group watchtower-app-instance to read secret-family in tenancy where any {target.vault.id = '$DEV_VAULT', target.vault.id = '$PROD_VAULT'}\"]"

echo "== GitHub Actions 전용 OCI 사용자 (배포 NSG 규칙만 관리)"
GHA_USER=$(ensure_api_user watchtower-github-actions watchtower-github-actions "GitHub Actions deploy NSG rule manager")
ensure_policy watchtower-github-actions-deploy-nsg "GitHub Actions opens runner SSH on deploy NSG" \
  '["Allow group watchtower-github-actions to manage network-security-groups in compartment watchtower-deploy"]'
ensure_api_key "$GHA_USER" github-actions-api-key

echo "== GitHub secrets / variables"
gh secret set OCI_CLI_USER -R $REPO -b "$GHA_USER"
gh secret set OCI_CLI_TENANCY -R $REPO -b "$T"
gh secret set OCI_CLI_FINGERPRINT -R $REPO -b "$(cat "$OUT_DIR/github-actions-api-key.fingerprint")"
gh secret set OCI_CLI_REGION -R $REPO -b "$REGION"
gh secret set OCI_CLI_KEY_CONTENT -R $REPO < "$OUT_DIR/github-actions-api-key.pem"
gh variable set DOKKU_HOST -R $REPO -b "$APP_PUBLIC_IP"
gh variable set OCI_DEPLOY_NSG_ID -R $REPO -b "$DEPLOY_NSG"

cat <<EOF
완료. 남은 수동 작업:
- DOKKU_SSH_PRIVATE_KEY secret 과 DOKKU_KNOWN_HOSTS 변수(앱 서버 호스트 키) 등록
- Vault 의 dev/prod/local secret 값 입력 (spring.datasource.*, watchtower.auth.naver.*)
- DB 계정 생성: ssh 로 infra/hosts/db/provision.sh 를 DEV_DB_PASSWORD, PROD_DB_PASSWORD 와 함께 실행
- DNS 를 $APP_PUBLIC_IP 로 지정하고 back-dev, back-prod, front-dev, front-prod 순서로 배포한 뒤 letsencrypt:enable
EOF
