package kr.byeongmin.watchtower.domain.subscription.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.subscription.enums.PlanTier
import kr.byeongmin.watchtower.global.entity.Base
import java.math.BigDecimal
import java.time.LocalDateTime

// 플랜 이용중이면서 상위 플랜을 추가결제 하는 경우 업그레이드가 됨
// 플랜 다운그레이드는 불가능하지만 환불은 남은 일수만큼 가능
@Entity
class Plan(
    val name: String,
    val price: BigDecimal,
    val durationDays: Int,
    @Enumerated(EnumType.STRING)
    val planTier: PlanTier,
    val availableFrom: LocalDateTime,
    val availableUntil: LocalDateTime?
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "plan_seq"
    )
    @SequenceGenerator(
        name = "plan_seq",
        sequenceName = "plan_id_seq",
        allocationSize = 30
    )
    val id: Long? = null
}