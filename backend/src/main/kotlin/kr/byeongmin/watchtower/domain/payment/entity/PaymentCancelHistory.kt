package kr.byeongmin.watchtower.domain.payment.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import kr.byeongmin.watchtower.global.entity.HistoryBase
import java.time.LocalDateTime

@Entity
class PaymentCancelHistory(
    @ManyToOne(fetch = FetchType.LAZY)
    private val paymentHistory: PaymentHistory,

    // 아래부터는 토스 관련 필드
    // 결제를 취소한 금액입니다.
    private val cancelAmount: Int,
    // 결제를 취소한 이유입니다. 최대 길이는 200자입니다.
    private val cancelReason: String,
    // 결제 취소가 일어난 날짜와 시간 정보입니다.
    private val canceledAt: LocalDateTime,
    // 취소 건의 키값입니다. 여러 건의 취소 거래를 구분하는 데 사용됩니다. 최대 길이는 64자입니다.
    private val transactionKey: String,
    @Column(columnDefinition = "TEXT")
    private val tossRawResponse: String
) : HistoryBase() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "payment_cancel_history_seq"
    )
    @SequenceGenerator(
        name = "payment_cancel_history_seq",
        sequenceName = "payment_cancel_history_id_seq",
        allocationSize = 30
    )
    val id: Long? = null
}