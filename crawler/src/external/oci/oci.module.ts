import { Module } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { ObjectStorageClient } from 'oci-objectstorage';
import type { RuntimeConfig } from '../../config/profile.js';
import { CrawlEnvironment, CrawlEnvironments } from '../../crawl/crawl-environment.js';
import { OciObjectStorage } from './oci-object-storage.js';
import { OciQueue } from './oci-queue.js';

@Module({
  providers: [
    {
      provide: CrawlEnvironments,
      inject: [ConfigService],
      useFactory: (configService: ConfigService<RuntimeConfig, true>) => {
        const authenticationDetailsProvider = configService.get('ociAuthentication', { infer: true });
        const objectStorageNamespace = configService.get('objectStorageNamespace', { infer: true });
        const objectStorageClient = new ObjectStorageClient({ authenticationDetailsProvider });
        return new CrawlEnvironments(
          configService.get('environments', { infer: true }).map((environmentConfig) => {
            return new CrawlEnvironment(
              environmentConfig.deploymentEnvironment,
              OciQueue.of(
                authenticationDetailsProvider,
                environmentConfig.requestQueueId,
                environmentConfig.requestQueueEndpoint
              ),
              OciQueue.of(
                authenticationDetailsProvider,
                environmentConfig.resultQueueId,
                environmentConfig.resultQueueEndpoint
              ),
              new OciObjectStorage(objectStorageClient, objectStorageNamespace, environmentConfig.bucketName)
            );
          })
        );
      }
    }
  ],
  exports: [CrawlEnvironments]
})
export class OciModule {}
