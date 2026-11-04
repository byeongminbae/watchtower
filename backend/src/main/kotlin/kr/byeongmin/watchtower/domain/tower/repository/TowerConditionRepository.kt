package kr.byeongmin.watchtower.domain.tower.repository

import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerCondition
import kr.byeongmin.watchtower.global.repository.BaseRepository

interface TowerConditionRepository : BaseRepository<TowerCondition, Long> {
    fun findByIdAndTower_IdAndTower_CreatedBy_IdAndDeletedAtIsNullAndTower_DeletedAtIsNull(
        id: Long,
        towerId: Long,
        memberId: Long
    ): TowerCondition?

    fun countByTowerAndDeletedAtIsNullAndTower_DeletedAtIsNull(tower: Tower): Long
}
