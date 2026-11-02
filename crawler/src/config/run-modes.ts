import type { EnvironmentConfig, RunModeConfig } from './config.js';

const QUEUE_ENDPOINT = 'https://cell-1.queue.messaging.ap-osaka-1.oci.oraclecloud.com';

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
    secretVaultId:
      'ocid1.vault.oc1.ap-osaka-1.fzvog2tkaadhq.abvwsljrjjhqqmg4rbn7dmzbn6sgar56lkn6yvav53mjgykufzgr7e6qbw7q'
  },
  local: {
    objectStorageNamespace: 'ax7wkezrucld',
    concurrency: 1,
    environments: [local],
    secretVaultId:
      'ocid1.vault.oc1.ap-osaka-1.fzvog2tgaafqy.abvwsljrjnlwob4t4xlgkhpprljovqbqz3kxpc552xfkocpn466xnscnq5oa'
  }
};
