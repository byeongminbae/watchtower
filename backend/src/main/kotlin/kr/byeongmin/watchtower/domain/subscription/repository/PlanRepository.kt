package kr.byeongmin.watchtower.domain.subscription.repository

import kr.byeongmin.watchtower.domain.subscription.entity.Plan
import kr.byeongmin.watchtower.domain.subscription.enums.PlanType
import kr.byeongmin.watchtower.global.repository.BaseRepository

interface PlanRepository : BaseRepository<Plan, Long> {
    fun findFirstByTypeAndDeletedAtIsNullOrderByIdAsc(type: PlanType): Plan?
}
