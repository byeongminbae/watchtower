export const CRAWLER_CONFIG = Symbol("CRAWLER_CONFIG")

export const CRAWL_ENVIRONMENT_NAMES = ["dev", "prod"] as const

export type CrawlEnvironmentName = (typeof CRAWL_ENVIRONMENT_NAMES)[number]

export type CrawlEnvironmentConfig = {
  name: CrawlEnvironmentName
  requestQueueId: string
  requestQueueEndpoint: string
  resultQueueId: string
  resultQueueEndpoint: string
  bucketName: string
}

export type CrawlerConfig = {
  ociRegion: string
  ociTenancyId: string
  ociUserId: string
  ociFingerprint: string
  ociPrivateKey: string
  objectStorageNamespace: string
  concurrency: number
  environments: CrawlEnvironmentConfig[]
}

/**
 * 크롤러 서버는 인스턴스 메타데이터 접근을 막아야 해서 Instance Principal 대신 전용 사용자의 API 키로 인증함
 *
 * 값이 하나라도 빠지면 빠진 이름을 모두 담아 기동을 멈춤
 */
export function readConfig(env: NodeJS.ProcessEnv): CrawlerConfig {
  const missingNames: string[] = []
  const required = (name: string): string => {
    const value = env[name]
    if (value === undefined || value.trim() === "") {
      missingNames.push(name)
      return ""
    }
    return value
  }

  const config: CrawlerConfig = {
    ociRegion: required("OCI_REGION"),
    ociTenancyId: required("OCI_TENANCY_ID"),
    ociUserId: required("OCI_USER_ID"),
    ociFingerprint: required("OCI_FINGERPRINT"),
    ociPrivateKey: required("OCI_PRIVATE_KEY"),
    objectStorageNamespace: required("OBJECT_STORAGE_NAMESPACE"),
    concurrency: Number(required("CRAWLER_CONCURRENCY")),
    environments: CRAWL_ENVIRONMENT_NAMES.map((name) => {
      const suffix = name.toUpperCase()
      return {
        name,
        requestQueueId: required(`CRAWL_REQUEST_QUEUE_ID_${suffix}`),
        requestQueueEndpoint: required(`CRAWL_REQUEST_QUEUE_ENDPOINT_${suffix}`),
        resultQueueId: required(`CRAWL_RESULT_QUEUE_ID_${suffix}`),
        resultQueueEndpoint: required(`CRAWL_RESULT_QUEUE_ENDPOINT_${suffix}`),
        bucketName: required(`OBJECT_STORAGE_BUCKET_${suffix}`)
      }
    })
  }

  if (missingNames.length > 0) {
    throw new Error(`필수 환경변수가 없음: ${missingNames.join(", ")}`)
  }
  if (!Number.isSafeInteger(config.concurrency) || config.concurrency <= 0) {
    throw new Error("CRAWLER_CONCURRENCY 는 1 이상의 정수여야 함")
  }

  return config
}
