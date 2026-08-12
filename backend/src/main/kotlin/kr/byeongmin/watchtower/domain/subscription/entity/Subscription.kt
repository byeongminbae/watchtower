package kr.byeongmin.watchtower.domain.subscription.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.entity.Base
import java.time.LocalDateTime

@Entity
class Subscription(
    @OneToOne(fetch = FetchType.LAZY)
    val member: Member,
    @ManyToOne(fetch = FetchType.LAZY)
    val currentPlan: Plan,
    val startedAt: LocalDateTime,
    val expiredAt: LocalDateTime
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "subscription_seq"
    )
    @SequenceGenerator(
        name = "subscription_seq",
        sequenceName = "subscription_id_seq",
        allocationSize = 30
    )
    val id: Long? = null
}