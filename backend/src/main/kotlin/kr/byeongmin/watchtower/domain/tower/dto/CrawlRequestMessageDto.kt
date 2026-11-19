package kr.byeongmin.watchtower.domain.tower.dto

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import java.time.LocalDateTime

/**
 * 크롤러 요청 큐에 넣는 크롤링 요청. 필드는 crawler 의 CrawlRequestMessage 와 같아야 함
 *
 * @property [triggeredByMemberId] 요청한 회원의 ID. 자동 요청이면 null이고 크롤러가 결과에 그대로 돌려줌
 * @property [previousTextHash] 직전 버전의 텍스트 해시 중 타워의 날짜 무시 설정에 맞는 것. 크롤러는 같은 설정으로 만든 해시와 비교함
 * @property [jobStartedAt] 백엔드가 이 크롤링 작업을 시작한 시각. 크롤러가 결과에 그대로 돌려줌
 */
data class CrawlRequestMessageDto(
    val towerId: Long,
    val url: String,
    val triggeredByMemberId: Long?,
    val previousTextHash: String?,
    val previousImageHash: String?,
    val jobStartedAt: LocalDateTime,
    val extraction: Extraction
) {
    /**
     * 추출할 요소 또는 옵션
     *  @property [detectionAreaSelector] null 이면 페이지 전체
     */
    data class Extraction(
        val detectionAreaSelector: String?,
        val collectImageHashes: Boolean,
        val ignoreDateTime: Boolean
    )

    companion object {
        /**
         * @param [latestSnapshot] 타워의 삭제되지 않은 가장 최근 스냅샷. 없으면 크롤러가 비교 없이 저장함
         *
         * 가장 최근 스냅샷의 감시 영역이 지금 설정과 다르면 해시를 비교할 수 없으므로, 직전 버전을 비워 크롤러가 저장을 생략하지 않게 함
         */
        fun from(
            tower: Tower,
            latestSnapshot: TowerSnapshot?,
            triggeredBy: Member?,
            jobStartedAt: LocalDateTime
        ): CrawlRequestMessageDto {
            val comparableSnapshot = latestSnapshot?.takeIf { it.hasSameDetectionArea(tower.detectionAreaSelector) }
            return CrawlRequestMessageDto(
                towerId = tower.id.ifNullThrow(),
                url = tower.url,
                extraction = Extraction(
                    detectionAreaSelector = tower.detectionAreaSelector,
                    collectImageHashes = tower.detectImageChange,
                    ignoreDateTime = tower.ignoreDateTime
                ),
                triggeredByMemberId = triggeredBy?.id,
                previousTextHash = comparableSnapshot?.selectTextHash(tower.ignoreDateTime),
                previousImageHash = comparableSnapshot?.imageHash,
                jobStartedAt = jobStartedAt
            )
        }
    }
}
