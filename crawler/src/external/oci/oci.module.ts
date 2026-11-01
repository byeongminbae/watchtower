import { Module } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { ObjectStorageClient } from 'oci-objectstorage';
import type { RuntimeConfig } from '../../config/config.js';
import { EnvironmentResource, EnvironmentResources } from '../../crawl/environment-resources.js';
import { HTML_STORAGE } from '../../crawl/interfaces/html-storage.interface.js';
import { OciHtmlStorage } from './oci-html-storage.js';
import { OciQueue } from './oci-queue.js';

@Module({
  providers: [
    {
      provide: HTML_STORAGE,
      inject: [ConfigService],
      useFactory: (configService: ConfigService<RuntimeConfig, true>) => {
        return new OciHtmlStorage(
          new ObjectStorageClient({
            authenticationDetailsProvider: configService.get('ociAuthentication', { infer: true })
          }),
          configService.get('objectStorageNamespace', { infer: true })
        );
      }
    },
    {
      provide: EnvironmentResources,
      inject: [ConfigService],
      useFactory: (configService: ConfigService<RuntimeConfig, true>) => {
        const authenticationDetailsProvider = configService.get('ociAuthentication', { infer: true });
        return new EnvironmentResources(
          configService.get('environments', { infer: true }).map((environmentConfig) => {
            return new EnvironmentResource(
              environmentConfig.name,
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
              environmentConfig.bucketName
            );
          })
        );
      }
    }
  ],
  exports: [HTML_STORAGE, EnvironmentResources]
})
export class OciModule {}
