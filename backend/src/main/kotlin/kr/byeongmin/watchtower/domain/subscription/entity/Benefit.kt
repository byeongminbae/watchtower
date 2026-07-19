package kr.byeongmin.watchtower.domain.subscription.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import kr.byeongmin.watchtower.domain.subscription.enums.BenefitType
import kr.byeongmin.watchtower.global.entity.Base

@Entity
class Benefit(
    @Enumerated(EnumType.STRING)
    private val benefitType: BenefitType
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