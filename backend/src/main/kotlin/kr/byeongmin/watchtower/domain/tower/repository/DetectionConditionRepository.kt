package kr.byeongmin.watchtower.domain.tower.repository

import kr.byeongmin.watchtower.domain.tower.entity.DetectionCondition
import kr.byeongmin.watchtower.global.repository.BaseRepository

interface DetectionConditionRepository : BaseRepository<DetectionCondition, Long> {
    fun findByIdAndTower_IdAndTower_CreatedBy_IdAndDeletedAtIsNullAndTower_DeletedAtIsNull(
        id: Long,
        towerId: Long,
        memberId: Long
    ): DetectionCondition?
}
