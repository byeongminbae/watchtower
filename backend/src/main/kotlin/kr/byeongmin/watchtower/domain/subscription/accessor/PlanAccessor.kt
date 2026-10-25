package kr.byeongmin.watchtower.domain.subscription.accessor

import kr.byeongmin.watchtower.domain.subscription.entity.Plan
import kr.byeongmin.watchtower.domain.subscription.enums.PlanType
import kr.byeongmin.watchtower.domain.subscription.repository.PlanRepository
import org.springframework.stereotype.Component

@Component
class PlanAccessor(
    private val planRepository: PlanRepository
) {
    fun findByType(planType: PlanType): Plan? {
        return planRepository.findFirstByTypeAndDeletedAtIsNullOrderByIdAsc(planType)
    }
}
