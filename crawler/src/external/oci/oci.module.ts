import { Module } from "@nestjs/common"
import { Region, SimpleAuthenticationDetailsProvider } from "oci-common"
import { ObjectStorageClient } from "oci-objectstorage"
import { CrawlerConfig } from "../../config/crawler-config.ts"
import { CrawlEnvironment, CrawlEnvironments } from "../../crawl/crawl-environment.ts"
import { HTML_STORAGE } from "../../crawl/interfaces/html-storage.ts"
import { OciHtmlStorage } from "./oci-html-storage.ts"
import { OciQueue } from "./oci-queue.ts"

@Module({
  providers: [
    {
      provide: SimpleAuthenticationDetailsProvider,
      inject: [CrawlerConfig],
      useFactory: (config: CrawlerConfig) => {
        return new SimpleAuthenticationDetailsProvider(
          config.ociTenancyId,
          config.ociUserId,
          config.ociFingerprint,
          config.ociPrivateKey,
          null,
          Region.fromRegionId(config.ociRegion)
        )
      }
    },
    {
      provide: HTML_STORAGE,
      inject: [CrawlerConfig, SimpleAuthenticationDetailsProvider],
      useFactory: (config: CrawlerConfig, authenticationDetailsProvider: SimpleAuthenticationDetailsProvider) => {
        return new OciHtmlStorage(new ObjectStorageClient({ authenticationDetailsProvider }), config.objectStorageNamespace)
      }
    },
    {
      provide: CrawlEnvironments,
      inject: [CrawlerConfig, SimpleAuthenticationDetailsProvider],
      useFactory: (config: CrawlerConfig, authenticationDetailsProvider: SimpleAuthenticationDetailsProvider) => {
        return new CrawlEnvironments(config.environments.map((environment) => {
          return new CrawlEnvironment(
            environment.name,
            OciQueue.of(authenticationDetailsProvider, environment.requestQueueId, environment.requestQueueEndpoint),
            OciQueue.of(authenticationDetailsProvider, environment.resultQueueId, environment.resultQueueEndpoint),
            environment.bucketName
          )
        }))
      }
    }
  ],
  exports: [HTML_STORAGE, CrawlEnvironments]
})
export class OciModule {}
