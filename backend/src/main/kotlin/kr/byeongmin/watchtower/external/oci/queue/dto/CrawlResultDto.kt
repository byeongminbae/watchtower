package kr.byeongmin.watchtower.external.oci.queue.dto

import java.time.LocalDateTime

/**
 * 크롤러가 결과 큐에 넣는 크롤링 결과. 필드는 crawler 의 CrawlResultMessage 와 같아야 함
 *
 * @property [resultStatus] FETCHED 는 페이지를 받아와 HTML 을 버킷의 [htmlObjectName] 에 저장한 결과(4xx, 5xx 응답 포함), FAILED 는 [errorCode] 로 실패한 결과
 * @property [crawlEndedAt] UTC
 * @property [triggeredByMemberId] 요청 메시지의 값을 크롤러가 그대로 돌려준 것. null 은 자동 요청
 */
data class CrawlResultDto(
    val towerId: Long,
    val url: String,
    val scheduledAt: String,
    val crawlStartedAt: LocalDateTime,
    val crawlEndedAt: LocalDateTime,
    val httpStatusCode: Int?,
    val finalUrl: String?,
    val htmlObjectName: String?,
    val resultStatus: CrawlResultStatus,
    val errorCode: String?,
    val triggeredByMemberId: Long?
) {
    enum class CrawlResultStatus {
        FETCHED, FAILED
    }
}
