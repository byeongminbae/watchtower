import { Module } from "@nestjs/common"
import { Region, SimpleAuthenticationDetailsProvider } from "oci-common"
import { ObjectStorageClient } from "oci-objectstorage"
import { QueueClient } from "oci-queue"
import { CRAWLER_CONFIG, type CrawlerConfig } from "../../config/crawler-config.ts"
import { CRAWL_ENVIRONMENTS, type CrawlEnvironment } from "../../crawl/crawl-environment.ts"
import { HTML_STORAGE } from "../../crawl/crawl.service.ts"
import { OciHtmlStorage } from "./oci-html-storage.ts"
import { OciQueue } from "./oci-queue.ts"

const AUTHENTICATION_DETAILS_PROVIDER = Symbol("AUTHENTICATION_DETAILS_PROVIDER")

@Module({
  providers: [
    {
      provide: AUTHENTICATION_DETAILS_PROVIDER,
      inject: [CRAWLER_CONFIG],
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
      inject: [CRAWLER_CONFIG, AUTHENTICATION_DETAILS_PROVIDER],
      useFactory: (config: CrawlerConfig, authenticationDetailsProvider: SimpleAuthenticationDetailsProvider) => {
        return new OciHtmlStorage(new ObjectStorageClient({ authenticationDetailsProvider }), config.objectStorageNamespace)
      }
    },
    {
      provide: CRAWL_ENVIRONMENTS,
      inject: [CRAWLER_CONFIG, AUTHENTICATION_DETAILS_PROVIDER],
      useFactory: (config: CrawlerConfig, authenticationDetailsProvider: SimpleAuthenticationDetailsProvider) => {
        const toQueue = (queueId: string, endpoint: string): OciQueue => {
          const client = new QueueClient({ authenticationDetailsProvider })
          client.endpoint = endpoint
          return new OciQueue(client, queueId)
        }

        return config.environments.map((environment): CrawlEnvironment => {
          return {
            name: environment.name,
            requestQueue: toQueue(environment.requestQueueId, environment.requestQueueEndpoint),
            resultQueue: toQueue(environment.resultQueueId, environment.resultQueueEndpoint),
            bucketName: environment.bucketName
          }
        })
      }
    }
  ],
  exports: [HTML_STORAGE, CRAWL_ENVIRONMENTS]
})
export class OciModule {}
