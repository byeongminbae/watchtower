package kr.byeongmin.watchtower.domain.subscription.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.global.entity.Base

@Entity
class PlanBenefit(
    @ManyToOne(fetch = FetchType.LAZY)
    val plan: Plan,
    @ManyToOne(fetch = FetchType.LAZY)
    val benefit: Benefit,
    val integerValue: Int
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