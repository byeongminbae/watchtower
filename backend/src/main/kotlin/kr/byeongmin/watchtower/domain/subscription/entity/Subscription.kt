package kr.byeongmin.watchtower.domain.subscription.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.entity.Base
import java.time.LocalDateTime

@Entity
class Subscription(
    @OneToOne(fetch = FetchType.LAZY)
    private val member: Member,
    @ManyToOne(fetch = FetchType.LAZY)
    private val currentPlan: Plan,
    private val startedAt: LocalDateTime,
    private val expiredAt: LocalDateTime
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