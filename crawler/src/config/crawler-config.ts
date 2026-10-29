export type CrawlEnvironmentName = (typeof CrawlerConfig.ENVIRONMENT_NAMES)[number]

export type CrawlEnvironmentConfig = {
  name: CrawlEnvironmentName
  requestQueueId: string
  requestQueueEndpoint: string
  resultQueueId: string
  resultQueueEndpoint: string
  bucketName: string
}

/**
 * 크롤러 서버는 인스턴스 메타데이터 접근을 막아야 해서 Instance Principal 대신 전용 사용자의 API 키로 인증함
 */
export class CrawlerConfig {
  static readonly ENVIRONMENT_NAMES = ["dev", "prod"] as const

  private constructor(
    readonly ociRegion: string,
    readonly ociTenancyId: string,
    readonly ociUserId: string,
    readonly ociFingerprint: string,
    readonly ociPrivateKey: string,
    readonly objectStorageNamespace: string,
    readonly concurrency: number,
    readonly environments: CrawlEnvironmentConfig[]
  ) {}

  /** 값이 하나라도 빠지면 빠진 이름을 모두 담아 기동을 멈춤 */
  static from(env: NodeJS.ProcessEnv): CrawlerConfig {
    const missingNames: string[] = []
    const required = (name: string): string => {
      const value = env[name]
      if (value === undefined || value.trim() === "") {
        missingNames.push(name)
        return ""
      }
      return value
    }

    const config = new CrawlerConfig(
      required("OCI_REGION"),
      required("OCI_TENANCY_ID"),
      required("OCI_USER_ID"),
      required("OCI_FINGERPRINT"),
      required("OCI_PRIVATE_KEY"),
      required("OBJECT_STORAGE_NAMESPACE"),
      Number(required("CRAWLER_CONCURRENCY")),
      CrawlerConfig.ENVIRONMENT_NAMES.map((name) => {
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
    )

    if (missingNames.length > 0) {
      throw new Error(`필수 환경변수가 없음: ${missingNames.join(", ")}`)
    }
    if (!CrawlerConfig.isPositiveInteger(config.concurrency)) {
      throw new Error("CRAWLER_CONCURRENCY 는 1 이상의 정수여야 함")
    }

    return config
  }

  private static isPositiveInteger(value: number): boolean {
    return Number.isSafeInteger(value) && value > 0
  }
}
