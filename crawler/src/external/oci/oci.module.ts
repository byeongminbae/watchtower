import { Module } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Region, SimpleAuthenticationDetailsProvider } from 'oci-common';
import { ObjectStorageClient } from 'oci-objectstorage';
import type { CrawlerConfig } from '../../config/crawler-config.js';
import { CrawlEnvironment, CrawlEnvironments } from '../../crawl/crawl-environment.js';
import { HTML_STORAGE } from '../../crawl/interfaces/html-storage.interface.js';
import { OciHtmlStorage } from './oci-html-storage.js';
import { OciQueue } from './oci-queue.js';

@Module({
  providers: [
    {
      provide: SimpleAuthenticationDetailsProvider,
      inject: [ConfigService],
      useFactory: (configService: ConfigService<CrawlerConfig, true>) => {
        return new SimpleAuthenticationDetailsProvider(
          configService.get('ociTenancyId', { infer: true }),
          configService.get('ociUserId', { infer: true }),
          configService.get('ociFingerprint', { infer: true }),
          configService.get('ociPrivateKey', { infer: true }),
          null,
          Region.fromRegionId(configService.get('ociRegion', { infer: true }))
        );
      }
    },
    {
      provide: HTML_STORAGE,
      inject: [ConfigService, SimpleAuthenticationDetailsProvider],
      useFactory: (
        configService: ConfigService<CrawlerConfig, true>,
        authenticationDetailsProvider: SimpleAuthenticationDetailsProvider
      ) => {
        return new OciHtmlStorage(
          new ObjectStorageClient({ authenticationDetailsProvider }),
          configService.get('objectStorageNamespace', { infer: true })
        );
      }
    },
    {
      provide: CrawlEnvironments,
      inject: [ConfigService, SimpleAuthenticationDetailsProvider],
      useFactory: (
        configService: ConfigService<CrawlerConfig, true>,
        authenticationDetailsProvider: SimpleAuthenticationDetailsProvider
      ) => {
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
