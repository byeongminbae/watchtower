import { defineConfig } from 'vitest/config';

const environmentVariables = Object.fromEntries(
  ['DEV', 'PROD'].flatMap((suffix) => [
    [`CRAWL_REQUEST_QUEUE_ID_${suffix}`, `request-${suffix}`],
    [`CRAWL_REQUEST_QUEUE_ENDPOINT_${suffix}`, 'http://127.0.0.1:9'],
    [`CRAWL_RESULT_QUEUE_ID_${suffix}`, `result-${suffix}`],
    [`CRAWL_RESULT_QUEUE_ENDPOINT_${suffix}`, 'http://127.0.0.1:9'],
    [`OBJECT_STORAGE_BUCKET_${suffix}`, `watchtower-snapshot-${suffix.toLowerCase()}`]
  ])
);

export default defineConfig({
  resolve: { tsconfigPaths: true },
  test: {
    globals: true,
    root: './',
    include: ['**/*.e2e-spec.ts'],
    setupFiles: ['reflect-metadata'],
    env: {
      OCI_REGION: 'ap-osaka-1',
      OCI_TENANCY_ID: 'e2e',
      OCI_USER_ID: 'e2e',
      OCI_FINGERPRINT: 'e2e',
      OCI_PRIVATE_KEY: 'e2e',
      OBJECT_STORAGE_NAMESPACE: 'e2e',
      CRAWLER_CONCURRENCY: '2',
      ...environmentVariables
    }
  }
});
