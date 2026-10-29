#!/usr/bin/env bash
# OCI 리소스 스크립트들이 함께 쓰는 값과 함수

T=ocid1.tenancy.oc1..aaaaaaaajrqij5qzv6e6fnfaq2pk2pjexmmgkbgabdwkyglw2kz2a3egbkna
REGION=ap-osaka-1
REPO=byeongminbae/watchtower
INFRA_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)

: "${ADMIN_CIDR:?관리자 IP 대역(예: 1.2.3.4/32)을 ADMIN_CIDR 로 지정해야 함}"
: "${OCI_USER_EMAIL:?OCI 사용자 생성에 필요한 이메일을 OCI_USER_EMAIL 로 지정해야 함}"
: "${SSH_PUBLIC_KEY_FILE:?인스턴스에 등록할 SSH 공개키 경로를 SSH_PUBLIC_KEY_FILE 로 지정해야 함}"
OUT_DIR=${OUT_DIR:-$HOME/.watchtower-infra}
mkdir -p "$OUT_DIR" && chmod 700 "$OUT_DIR"

# 조회 결과가 없으면 OCI CLI 는 아무것도 출력하지 않으므로 jq -s 로 빈 입력을 처리함
first() { jq -rs '.[0].data[0].id // empty'; }
first_item() { jq -rs '.[0].data.items[0].id // empty'; }
count() { jq -rs '.[0].data // [] | length'; }

availability_domain() {
  oci iam availability-domain list -c $T --query 'data[0].name' --raw-output
}

deploy_compartment() {
  oci iam compartment list -c $T --name watchtower-deploy --lifecycle-state ACTIVE | first
}

latest_ubuntu_image() {
  oci compute image list -c $T --operating-system "Canonical Ubuntu" --operating-system-version "26.04" \
    --shape VM.Standard.A1.Flex --sort-by TIMECREATED --sort-order DESC --limit 1 | first
}

tcp_rule() { # direction source sourceType port description
  printf '{"direction":"%s","protocol":"6","source":"%s","sourceType":"%s","tcpOptions":{"destinationPortRange":{"min":%s,"max":%s}},"description":"%s"}' \
    "$1" "$2" "$3" "$4" "$4" "$5"
}

ensure_nsg() { # compartment vcn name
  local id
  id=$(oci network nsg list -c "$1" --vcn-id "$2" --display-name "$3" | first)
  if [ -z "$id" ]; then
    id=$(oci network nsg create -c "$1" --vcn-id "$2" --display-name "$3" --wait-for-state AVAILABLE --query data.id --raw-output)
  fi
  echo "$id"
}

ensure_nsg_rules() { # nsg rules-json
  if [ "$(oci network nsg rules list --nsg-id "$1" --all | count)" = "0" ]; then
    oci network nsg rules add --nsg-id "$1" --security-rules "$2" >/dev/null
  fi
}

ensure_policy() { # name description statements-json
  if [ -z "$(oci iam policy list -c $T --name "$1" | first)" ]; then
    oci iam policy create -c $T --name "$1" --description "$2" --statements "$3" >/dev/null
  fi
}

ensure_api_user() { # name group description
  local user group
  user=$(oci iam user list --name "$1" | first)
  if [ -z "$user" ]; then
    user=$(oci iam user create --name "$1" --description "$3" --email "$OCI_USER_EMAIL" --wait-for-state ACTIVE --query data.id --raw-output)
  fi
  oci iam user update-user-capabilities --user-id "$user" --can-use-api-keys true --can-use-auth-tokens false \
    --can-use-console-password false --can-use-customer-secret-keys false --can-use-smtp-credentials false \
    --can-use-db-credentials false --can-use-o-auth2-client-credentials false >/dev/null
  group=$(oci iam group list --name "$2" | first)
  if [ -z "$group" ]; then
    group=$(oci iam group create --name "$2" --description "$3" --wait-for-state ACTIVE --query data.id --raw-output)
  fi
  if ! oci iam group list-users --group-id "$group" --all | jq -e --arg id "$user" '.data[] | select(.id == $id)' >/dev/null; then
    oci iam group add-user --group-id "$group" --user-id "$user" >/dev/null
  fi
  echo "$user"
}

# 새로 만든 키만 파일로 남김. 이미 등록된 키의 개인키는 다시 받을 수 없으므로 잃어버렸으면 OCI 에서 키를 지우고 다시 실행함
ensure_api_key() { # user name
  if [ ! -f "$OUT_DIR/$2.pem" ]; then
    openssl genrsa -out "$OUT_DIR/$2.pem" 2048 2>/dev/null
    openssl rsa -in "$OUT_DIR/$2.pem" -pubout -out "$OUT_DIR/$2.pub.pem" 2>/dev/null
    oci iam user api-key upload --user-id "$1" --key-file "$OUT_DIR/$2.pub.pem" --query data.fingerprint --raw-output > "$OUT_DIR/$2.fingerprint"
  fi
}

# 인스턴스 첫 부팅 때 실행할 서버 설정 스크립트에 환경변수를 붙여 cloud-init user data 를 만듦
# user data 는 인스턴스 안에서 메타데이터로 읽을 수 있으므로 비밀값은 넣지 않음
user_data_file() { # provision-script NAME=VALUE...
  local file
  file=$(mktemp)
  {
    echo "#!/usr/bin/env bash"
    for assignment in "${@:2}"; do
      printf 'export %q\n' "$assignment"
    done
    tail -n +2 "$1"
  } > "$file"
  echo "$file"
}

ensure_instance() { # name subnet private-ip assign-public-ip nsg-ids-json user-data-file
  local id
  id=$(oci compute instance list -c $T --display-name "$1" --lifecycle-state RUNNING | first)
  if [ -z "$id" ]; then
    id=$(oci compute instance launch -c $T --availability-domain "$(availability_domain)" --display-name "$1" \
      --shape VM.Standard.A1.Flex --shape-config '{"ocpus":1,"memoryInGBs":6}' --image-id "$(latest_ubuntu_image)" \
      --boot-volume-size-in-gbs 50 --subnet-id "$2" --private-ip "$3" --assign-public-ip "$4" --nsg-ids "$5" \
      --ssh-authorized-keys-file "$SSH_PUBLIC_KEY_FILE" --user-data-file "$6" \
      --wait-for-state RUNNING --query data.id --raw-output)
  fi
  echo "$id"
}
