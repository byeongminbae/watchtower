package kr.byeongmin.watchtower.domain.tower.dto

import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import java.time.LocalDateTime

data class TowerSnapshotResponseDto(
    val id: Long,
    val aiDiffSummary: String?,
    val notifiedAt: LocalDateTime?
) {
    companion object {
        fun from(towerSnapshot: TowerSnapshot): TowerSnapshotResponseDto {
            return TowerSnapshotResponseDto(
                id = towerSnapshot.id.ifNullThrow(),
                aiDiffSummary = towerSnapshot.aiDiffSummary,
                notifiedAt = towerSnapshot.notifiedAt
            )
        }
    }
}
