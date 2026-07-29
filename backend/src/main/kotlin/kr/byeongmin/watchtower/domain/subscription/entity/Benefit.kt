package kr.byeongmin.watchtower.domain.subscription.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.subscription.enums.BenefitType
import kr.byeongmin.watchtower.global.entity.Base

@Entity
class Benefit(
    @Enumerated(EnumType.STRING)
    val benefitType: BenefitType
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "benefit_seq"
    )
    @SequenceGenerator(
        name = "benefit_seq",
        sequenceName = "benefit_id_seq",
        allocationSize = 30
    )
    val id: Long? = null
}