package kr.byeongmin.watchtower.domain.tower.dto

import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import kr.byeongmin.watchtower.global.utils.isNotNull
import java.time.LocalDateTime

/** @property [detectedAt] 백엔드가 직전 버전과 비교해 변경을 감지하고 스냅샷을 저장한 시각(UTC) */
data class TowerSnapshotResponseDto(
    val id: Long,
    val detectedAt: LocalDateTime,
    val hasHighlightedScreenshot: Boolean,
    val aiDiffSummary: String?,
    val notifiedAt: LocalDateTime?
) {
    companion object {
        fun from(towerSnapshot: TowerSnapshot): TowerSnapshotResponseDto {
            return TowerSnapshotResponseDto(
                id = towerSnapshot.id.ifNullThrow(),
                detectedAt = towerSnapshot.createdAt.ifNullThrow(),
                hasHighlightedScreenshot = towerSnapshot.highlightedScreenshotObjectName.isNotNull(),
                aiDiffSummary = towerSnapshot.aiDiffSummary,
                notifiedAt = towerSnapshot.notifiedAt
            )
        }
    }
}
