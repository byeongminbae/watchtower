package kr.byeongmin.watchtower.external.oci.queue.dto

import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * 크롤러 요청 큐에 넣는 크롤링 요청. 필드는 crawler 의 CrawlRequestMessage 와 같아야 함
 *
 * @property [scheduledAt] 실행 예정 시각(ISO 8601, UTC, 밀리초까지). 크롤러가 towerId 와 함께 HTML 객체 이름을 만드는 데 씀
 * @property [triggeredByMemberId] 크롤링을 요청한 회원. null 은 자동 요청. 크롤러가 결과에 그대로 돌려줌
 * @property [previousHtmlSha256] 직전 버전 HTML 의 SHA-256. 크롤러는 받아온 HTML 이 이 값과 같으면 버킷에 저장하지 않음. null 이면 비교 없이 저장함
 */
data class CrawlRequestDto(
    val towerId: Long,
    val url: String,
    val scheduledAt: String,
    val triggeredByMemberId: Long?,
    val previousHtmlSha256: String?
) {
    companion object {
        /** @param [latestSnapshot] 타워의 삭제되지 않은 가장 최근 스냅샷. 없으면 크롤러가 비교 없이 저장함 */
        fun from(
            tower: Tower,
            latestSnapshot: TowerSnapshot?,
            scheduledAt: LocalDateTime,
            triggeredByMemberId: Long?
        ): CrawlRequestDto {
            return CrawlRequestDto(
                towerId = tower.id.ifNullThrow(),
                url = tower.url,
                scheduledAt = scheduledAt.truncatedTo(ChronoUnit.MILLIS).toInstant(ZoneOffset.UTC).toString(),
                triggeredByMemberId = triggeredByMemberId,
                previousHtmlSha256 = latestSnapshot?.htmlSha256
            )
        }
    }
}
