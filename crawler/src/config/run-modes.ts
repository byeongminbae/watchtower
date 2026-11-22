import type { EnvironmentConfig, RunModeConfig } from './profile.js';

const QUEUE_ENDPOINT = 'https://cell-1.queue.messaging.ap-osaka-1.oci.oraclecloud.com';
const COMMON_SECRET_VAULT_ID = 'ocid1.vault.oc1.ap-osaka-1.fzvqakbgaafqe.abvwsljrroq6wci3nv5uz6a2y4hii4kzzmxtgr26mi5mjygc5zm2isq4budq';

const dev: EnvironmentConfig = {
  deploymentEnvironment: 'dev',
  requestQueueId: 'ocid1.queue.oc1.ap-osaka-1.amaaaaaaw36r47iaur3pqw4olpn2yhkhhhjyjzqpf26asmegj7rbyuhful2a',
  requestQueueEndpoint: QUEUE_ENDPOINT,
  resultQueueId: 'ocid1.queue.oc1.ap-osaka-1.amaaaaaaw36r47iaodfs4xklavbthpbpaamu5jta5ovojxbv2rvvit2ele4q',
  resultQueueEndpoint: QUEUE_ENDPOINT,
  bucketName: 'watchtower-snapshot-dev'
};

const prod: EnvironmentConfig = {
  deploymentEnvironment: 'prod',
  requestQueueId: 'ocid1.queue.oc1.ap-osaka-1.amaaaaaaw36r47iasrufpfd2x7pvibd3ouxe3tocsfiht3znod4rxb4ap7hq',
  requestQueueEndpoint: QUEUE_ENDPOINT,
  resultQueueId: 'ocid1.queue.oc1.ap-osaka-1.amaaaaaaw36r47iakszih55zlhcigegkju6bnyzawtb4iqdvgzamywqzrjja',
  resultQueueEndpoint: QUEUE_ENDPOINT,
  bucketName: 'watchtower-snapshot-prod'
};

const local: EnvironmentConfig = {
  deploymentEnvironment: 'local',
  requestQueueId: 'ocid1.queue.oc1.ap-osaka-1.amaaaaaaw36r47iayiwr7o4un42ywx3u2dlbhjc55ykl4neing3vpqbss7ya',
  requestQueueEndpoint: QUEUE_ENDPOINT,
  resultQueueId: 'ocid1.queue.oc1.ap-osaka-1.amaaaaaaw36r47iae4shyzwkan7zjiwsgio2ezjbluws53x2kiejsflqxhkq',
  resultQueueEndpoint: QUEUE_ENDPOINT,
  bucketName: 'watchtower-snapshot-local'
};

export const RUN_MODES: Record<string, RunModeConfig> = {
  server: {
    objectStorageNamespace: 'ax7wkezrucld',
    concurrency: 2,
    environments: [dev, prod],
    secretVaultId: COMMON_SECRET_VAULT_ID
  },
  local: {
    objectStorageNamespace: 'ax7wkezrucld',
    concurrency: 1,
    environments: [local],
    secretVaultId: COMMON_SECRET_VAULT_ID
  }
};
