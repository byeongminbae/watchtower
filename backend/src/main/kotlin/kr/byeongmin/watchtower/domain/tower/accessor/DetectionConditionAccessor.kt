package kr.byeongmin.watchtower.domain.tower.accessor

import kr.byeongmin.watchtower.domain.tower.entity.DetectionCondition
import kr.byeongmin.watchtower.domain.tower.repository.DetectionConditionQuerydslRepository
import kr.byeongmin.watchtower.domain.tower.repository.DetectionConditionRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class DetectionConditionAccessor(
    private val detectionConditionRepository: DetectionConditionRepository,
    private val detectionConditionQuerydslRepository: DetectionConditionQuerydslRepository
) {
    fun getOwnedDetectionConditions(towerId: Long, memberId: Long): List<DetectionCondition> {
        return detectionConditionQuerydslRepository.findByTowerIdAndMemberId(towerId, memberId)
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }

    fun getOwnedDetectionCondition(conditionId: Long, towerId: Long, memberId: Long): DetectionCondition {
        return detectionConditionRepository.findByIdAndTower_IdAndTower_CreatedBy_IdAndDeletedAtIsNullAndTower_DeletedAtIsNull(
            conditionId,
            towerId,
            memberId
        ) ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }

    fun save(condition: DetectionCondition): DetectionCondition {
        return detectionConditionRepository.save(condition)
    }
}
