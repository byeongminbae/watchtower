package kr.byeongmin.watchtower.domain.tower.dto

import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import java.time.LocalDateTime

data class TowerSnapshotResponseDto(
    val id: Long,
    val htmlContentUrl: String,
    val htmlContentDiffUrl: String?,
    val screenshotUrl: String?,
    val screenshotDiffUrl: String?,
    val aiDiffSummary: String?,
    val notifiedAt: LocalDateTime?
) {
    companion object {
        fun from(towerSnapshot: TowerSnapshot): TowerSnapshotResponseDto {
            return TowerSnapshotResponseDto(
                id = towerSnapshot.id.ifNullThrow(),
                htmlContentUrl = towerSnapshot.htmlContentUrl,
                htmlContentDiffUrl = towerSnapshot.htmlContentDiffUrl,
                screenshotUrl = towerSnapshot.screenshotUrl,
                screenshotDiffUrl = towerSnapshot.screenshotDiffUrl,
                aiDiffSummary = towerSnapshot.aiDiffSummary,
                notifiedAt = towerSnapshot.notifiedAt
            )
        }
    }
}
