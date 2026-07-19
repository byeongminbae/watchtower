package kr.byeongmin.watchtower.domain.payment.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.subscription.enums.PlanTier
import kr.byeongmin.watchtower.global.entity.HistoryBase
import java.math.BigDecimal

/**
 * 현재 상황을 봤을 때 결제 수단이 대부분의 카드사를 지원하는 Toss 이외에는 나오지 않을 가능성이 매우 높습니다.
 * 결제 대상도 Plan 외에는 나오지 않을 가능성이 높고요
 * 초기 설계대로 하기엔 확률도 적은 "확장 가능성"에 시간을 낭비하는 느낌이 듭니다.(복잡도 증가)
 * 일단 PaymentHistory 와 PaymentCancelHistory 둘로만 나누고 필요한 시기에 확장을 하는것이 나을 것 같습니다.
 */
// TODO: 추후 쿠폰 엔티티나 적립금 사용 필드를 따로 만들어야함
@Entity
class PaymentHistory(
    @ManyToOne(fetch = FetchType.LAZY)
    private val member: Member,
    private val planName: String,
    private val planPrice: BigDecimal,
    private val planDurationDays: Int,
    @Enumerated(EnumType.STRING)
    private val planTier: PlanTier,
    @Column(columnDefinition = "TEXT")
    private val tossRawResponse: String
) : HistoryBase() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "payment_history_seq"
    )
    @SequenceGenerator(
        name = "payment_history_seq",
        sequenceName = "payment_history_id_seq",
        allocationSize = 30
    )
    val id: Long? = null
}