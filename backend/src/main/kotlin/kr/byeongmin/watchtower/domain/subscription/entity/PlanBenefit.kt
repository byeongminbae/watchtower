package kr.byeongmin.watchtower.domain.subscription.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import kr.byeongmin.watchtower.global.entity.Base

@Entity
class PlanBenefit(
    @ManyToOne(fetch = FetchType.LAZY)
    private val plan: Plan,
    @ManyToOne(fetch = FetchType.LAZY)
    private val benefit: Benefit,

    private val integerValue: Int
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "plan_benefit_seq"
    )
    @SequenceGenerator(
        name = "plan_benefit_seq",
        sequenceName = "plan_benefit_id_seq",
        allocationSize = 30
    )
    val id: Long? = null
}