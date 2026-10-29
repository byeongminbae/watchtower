import { Module } from "@nestjs/common"
import { Region, SimpleAuthenticationDetailsProvider } from "oci-common"
import { ObjectStorageClient } from "oci-objectstorage"
import { QueueClient } from "oci-queue"
import { CrawlerConfig } from "../../config/crawler-config.ts"
import { CrawlEnvironment, CrawlEnvironments } from "../../crawl/crawl-environment.ts"
import { HtmlStorage } from "../../crawl/crawl.service.ts"
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
      provide: HtmlStorage,
      inject: [CrawlerConfig, SimpleAuthenticationDetailsProvider],
      useFactory: (config: CrawlerConfig, authenticationDetailsProvider: SimpleAuthenticationDetailsProvider) => {
        return new OciHtmlStorage(new ObjectStorageClient({ authenticationDetailsProvider }), config.objectStorageNamespace)
      }
    },
    {
      provide: CrawlEnvironments,
      inject: [CrawlerConfig, SimpleAuthenticationDetailsProvider],
      useFactory: (config: CrawlerConfig, authenticationDetailsProvider: SimpleAuthenticationDetailsProvider) => {
        const toQueue = (queueId: string, endpoint: string): OciQueue => {
          const client = new QueueClient({ authenticationDetailsProvider })
          client.endpoint = endpoint
          return new OciQueue(client, queueId)
        }

        return new CrawlEnvironments(config.environments.map((environment) => {
          return new CrawlEnvironment(
            environment.name,
            toQueue(environment.requestQueueId, environment.requestQueueEndpoint),
            toQueue(environment.resultQueueId, environment.resultQueueEndpoint),
            environment.bucketName
          )
        }))
      }
    }
  ],
  exports: [HtmlStorage, CrawlEnvironments]
})
export class OciModule {}
