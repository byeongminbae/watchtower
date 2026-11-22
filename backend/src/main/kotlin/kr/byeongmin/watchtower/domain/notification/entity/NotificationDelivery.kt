package kr.byeongmin.watchtower.domain.notification.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.auth.entity.SocialAccount
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.utils.isNotNull
import org.springframework.util.backoff.BackOffExecution
import org.springframework.util.backoff.ExponentialBackOff
import java.time.Duration
import java.time.LocalDateTime

/**
 * 한번 알림에 유저가 연결한 [SocialAccount] 갯수만큼 생성되는 다대다 연결 엔티티
 */
@Entity
class NotificationDelivery private constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    val notification: Notification,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    val socialAccount: SocialAccount,
    nextAttemptAt: LocalDateTime
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "notification_delivery_seq"
    )
    @SequenceGenerator(
        name = "notification_delivery_seq",
        sequenceName = "notification_delivery_id_seq",
        allocationSize = 30
    )
    override val id: Long? = null

    var nextAttemptAt: LocalDateTime = nextAttemptAt
        protected set

    var failedAttemptCount: Int = 0
        protected set

    var succeededAt: LocalDateTime? = null
        protected set

    var abandonedAt: LocalDateTime? = null
        protected set

    fun markSucceeded(succeededAt: LocalDateTime) {
        this.succeededAt = succeededAt
    }

    fun markAbandoned(abandonedAt: LocalDateTime) {
        this.abandonedAt = abandonedAt
    }

    fun isAbandoned(): Boolean {
        return abandonedAt.isNotNull()
    }

    fun scheduleRetry(failedAt: LocalDateTime) {
        failedAttemptCount++
        val retryDelayMillis = getRetryDelayMillis()

        if (retryDelayMillis == BackOffExecution.STOP) {
            markAbandoned(failedAt)
            return
        }

        nextAttemptAt = failedAt.plus(Duration.ofMillis(retryDelayMillis))
    }

    private fun getRetryDelayMillis(): Long {
        val backOffExecution = RETRY_BACK_OFF.start()
        var retryDelayMillis = BackOffExecution.STOP

        repeat(failedAttemptCount) {
            retryDelayMillis = backOffExecution.nextBackOff()
        }
        return retryDelayMillis
    }

    companion object {
        const val MAX_RETRY_COUNT = 5L // TODO: 맥시멈 넘기면ㄴ 사용자가 알수있게 프론트에서 타워에 표시해야할듯
        const val RETRY_MULTIPLIER = 2.0
        val RETRY_INITIAL_INTERVAL: Duration = Duration.ofMinutes(1)
        val RETRY_MAX_INTERVAL: Duration = Duration.ofHours(1)
        val RETRY_JITTER: Duration = RETRY_INITIAL_INTERVAL.dividedBy(4)

        private val RETRY_BACK_OFF = ExponentialBackOff(RETRY_INITIAL_INTERVAL.toMillis(), RETRY_MULTIPLIER).apply {
            // 지정하지 않으면 Spring 기본값 30초로 잘려 모든 대기가 30초가 됨
            maxInterval = RETRY_MAX_INTERVAL.toMillis()
            jitter = RETRY_JITTER.toMillis()
            maxAttempts = MAX_RETRY_COUNT
        }

        fun of(
            notification: Notification,
            socialAccount: SocialAccount,
            nextAttemptAt: LocalDateTime
        ): NotificationDelivery {
            return NotificationDelivery(
                notification = notification,
                socialAccount = socialAccount,
                nextAttemptAt = nextAttemptAt
            )
        }
    }
}
