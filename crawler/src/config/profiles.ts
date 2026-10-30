import type { EnvironmentConfig, Settings } from './config.js';

const QUEUE_ENDPOINT = 'https://cell-1.queue.messaging.ap-osaka-1.oci.oraclecloud.com';

const dev: EnvironmentConfig = {
  name: 'dev',
  requestQueueId: 'ocid1.queue.oc1.ap-osaka-1.amaaaaaaw36r47iaur3pqw4olpn2yhkhhhjyjzqpf26asmegj7rbyuhful2a',
  requestQueueEndpoint: QUEUE_ENDPOINT,
  resultQueueId: 'ocid1.queue.oc1.ap-osaka-1.amaaaaaaw36r47iaodfs4xklavbthpbpaamu5jta5ovojxbv2rvvit2ele4q',
  resultQueueEndpoint: QUEUE_ENDPOINT,
  bucketName: 'watchtower-snapshot-dev'
};

const prod: EnvironmentConfig = {
  name: 'prod',
  requestQueueId: 'ocid1.queue.oc1.ap-osaka-1.amaaaaaaw36r47iasrufpfd2x7pvibd3ouxe3tocsfiht3znod4rxb4ap7hq',
  requestQueueEndpoint: QUEUE_ENDPOINT,
  resultQueueId: 'ocid1.queue.oc1.ap-osaka-1.amaaaaaaw36r47iakszih55zlhcigegkju6bnyzawtb4iqdvgzamywqzrjja',
  resultQueueEndpoint: QUEUE_ENDPOINT,
  bucketName: 'watchtower-snapshot-prod'
};

export const PROFILES: Record<string, Settings> = {
  server: {
    objectStorageNamespace: 'ax7wkezrucld',
    concurrency: 2,
    environments: [dev, prod]
  },
  local: {
    objectStorageNamespace: 'ax7wkezrucld',
    concurrency: 1,
    environments: [dev]
  }
};
