package kr.byeongmin.watchtower.external.oci.queue.dto

import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * 크롤러 요청 큐에 넣는 크롤링 요청. 필드는 crawler 의 CrawlRequestMessage 와 같아야 함
 *
 * @property [scheduledAt] 실행 예정 시각(ISO 8601, UTC, 밀리초까지). 크롤러가 towerId 와 함께 HTML 객체 이름을 만드는 데 씀
 * @property [triggeredByMemberId] 크롤링을 요청한 회원. null 은 자동 요청. 크롤러가 결과에 그대로 돌려줌
 */
data class CrawlRequestDto(
    val towerId: Long,
    val url: String,
    val scheduledAt: String,
    val triggeredByMemberId: Long?
) {
    companion object {
        fun from(tower: Tower, scheduledAt: LocalDateTime, triggeredByMemberId: Long?): CrawlRequestDto {
            return CrawlRequestDto(
                towerId = tower.id.ifNullThrow(),
                url = tower.url,
                scheduledAt = scheduledAt.truncatedTo(ChronoUnit.MILLIS).toInstant(ZoneOffset.UTC).toString(),
                triggeredByMemberId = triggeredByMemberId
            )
        }
    }
}
