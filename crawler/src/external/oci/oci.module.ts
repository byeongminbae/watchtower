import { Module } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { ObjectStorageClient } from 'oci-objectstorage';
import type { Config } from '../../config/config.js';
import { CrawlEnvironment, CrawlEnvironments } from '../../crawl/crawl-environment.model.js';
import { HTML_STORAGE } from '../../crawl/interfaces/html-storage.interface.js';
import { OciHtmlStorage } from './oci-html-storage.js';
import { OciQueue } from './oci-queue.js';

@Module({
  providers: [
    {
      provide: HTML_STORAGE,
      inject: [ConfigService],
      useFactory: (configService: ConfigService<Config, true>) => {
        return new OciHtmlStorage(
          new ObjectStorageClient({
            authenticationDetailsProvider: configService.get('ociAuthentication', { infer: true })
          }),
          configService.get('objectStorageNamespace', { infer: true })
        );
      }
    },
    {
      provide: CrawlEnvironments,
      inject: [ConfigService],
      useFactory: (configService: ConfigService<Config, true>) => {
        const authenticationDetailsProvider = configService.get('ociAuthentication', { infer: true });
        return new CrawlEnvironments(
          configService.get('environments', { infer: true }).map((environment) => {
            return new CrawlEnvironment(
              environment.name,
              OciQueue.of(authenticationDetailsProvider, environment.requestQueueId, environment.requestQueueEndpoint),
              OciQueue.of(authenticationDetailsProvider, environment.resultQueueId, environment.resultQueueEndpoint),
              environment.bucketName
            );
          })
        );
      }
    }
  ],
  exports: [HTML_STORAGE, CrawlEnvironments]
})
export class OciModule {}
