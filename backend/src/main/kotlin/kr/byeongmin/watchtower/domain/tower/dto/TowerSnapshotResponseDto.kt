package kr.byeongmin.watchtower.domain.tower.dto

import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import java.time.LocalDateTime

data class TowerSnapshotResponseDto(
    val id: Long,
    val highlightedScreenshotUrl: String?,
    val aiDiffSummary: String?,
    val notifiedAt: LocalDateTime?
) {
    companion object {
        /** @param [highlightedScreenshotUrl] 하이라이트 스크린샷을 읽을 수 있는 PAR URL. 하이라이트 스크린샷이 없으면 null */
        fun from(towerSnapshot: TowerSnapshot, highlightedScreenshotUrl: String?): TowerSnapshotResponseDto {
            return TowerSnapshotResponseDto(
                id = towerSnapshot.id.ifNullThrow(),
                highlightedScreenshotUrl = highlightedScreenshotUrl,
                aiDiffSummary = towerSnapshot.aiDiffSummary,
                notifiedAt = towerSnapshot.notifiedAt
            )
        }
    }
}
