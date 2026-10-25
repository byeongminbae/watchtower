package kr.byeongmin.watchtower.domain.tower.accessor

import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerCondition
import kr.byeongmin.watchtower.domain.tower.repository.TowerConditionQuerydslRepository
import kr.byeongmin.watchtower.domain.tower.repository.TowerConditionRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class TowerConditionAccessor(
    private val towerConditionRepository: TowerConditionRepository,
    private val towerConditionQuerydslRepository: TowerConditionQuerydslRepository
) {
    fun getByTowerId(towerId: Long): List<TowerCondition> {
        return towerConditionRepository.findByTower_IdAndDeletedAtIsNullAndTower_DeletedAtIsNull(towerId)
    }

    fun getOwnedTowerConditions(towerId: Long, memberId: Long): List<TowerCondition> {
        return towerConditionQuerydslRepository.findByTowerIdAndMemberId(towerId, memberId)
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }

    fun getOwnedCondition(conditionId: Long, towerId: Long, memberId: Long): TowerCondition {
        return towerConditionRepository.findByIdAndTower_IdAndTower_CreatedBy_IdAndDeletedAtIsNullAndTower_DeletedAtIsNull(
            conditionId,
            towerId,
            memberId
        ) ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }

    fun countByTower(tower: Tower): Long {
        return towerConditionRepository.countByTowerAndDeletedAtIsNullAndTower_DeletedAtIsNull(tower)
    }

    fun save(condition: TowerCondition): TowerCondition {
        return towerConditionRepository.save(condition)
    }
}
