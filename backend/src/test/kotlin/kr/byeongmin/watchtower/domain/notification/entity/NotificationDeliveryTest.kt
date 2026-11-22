package kr.byeongmin.watchtower.domain.notification.entity

import kr.byeongmin.watchtower.domain.auth.entity.SocialAccount
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationDeliveryTest {
    private val notification = mock<Notification>()
    private val socialAccount = mock<SocialAccount>()
    private val createdAt = LocalDateTime.of(2026, 11, 21, 3, 0)

    @Test
    fun `새 발송은 만든 시각에 바로 보낼 수 있음`() {
        /** when */
        val notificationDelivery = NotificationDelivery.of(notification, socialAccount, createdAt)

        /** then */
        assertEquals(createdAt, notificationDelivery.nextAttemptAt)
        assertEquals(0, notificationDelivery.failedAttemptCount)
        assertNull(notificationDelivery.succeededAt)
        assertFalse(notificationDelivery.isAbandoned())
    }

    @Test
    fun `발송에 성공하면 성공 시각을 남김`() {
        /** given */
        val notificationDelivery = NotificationDelivery.of(notification, socialAccount, createdAt)
        val succeededAt = createdAt.plusSeconds(5)

        /** when */
        notificationDelivery.markSucceeded(succeededAt)

        /** then */
        assertEquals(succeededAt, notificationDelivery.succeededAt)
    }

    @Test
    fun `보내지 못한 발송은 실패할 때마다 지수적으로 늘어난 대기 시간을 무작위로 흔들어 다음 시도 시각을 미룸`() {
        /** given */
        val notificationDelivery = NotificationDelivery.of(notification, socialAccount, createdAt)
        val firstFailedAt = createdAt.plusSeconds(5)
        val secondFailedAt = firstFailedAt.plusMinutes(1)

        /** when */
        notificationDelivery.scheduleRetry(firstFailedAt)
        val firstNextAttemptAt = notificationDelivery.nextAttemptAt
        notificationDelivery.scheduleRetry(secondFailedAt)

        /** then */
        val firstRetryDelay = NotificationDelivery.RETRY_INITIAL_INTERVAL
        val secondRetryDelay = firstRetryDelay.multipliedBy(NotificationDelivery.RETRY_MULTIPLIER.toLong())
        val secondRetryJitter = NotificationDelivery.RETRY_JITTER.multipliedBy(NotificationDelivery.RETRY_MULTIPLIER.toLong())
        assertTrue(
            firstNextAttemptAt in
                firstFailedAt.plus(firstRetryDelay)..firstFailedAt.plus(firstRetryDelay).plus(NotificationDelivery.RETRY_JITTER)
        )
        assertTrue(
            notificationDelivery.nextAttemptAt in
                secondFailedAt.plus(secondRetryDelay).minus(secondRetryJitter)..secondFailedAt.plus(secondRetryDelay).plus(secondRetryJitter)
        )
        assertEquals(2, notificationDelivery.failedAttemptCount)
        assertFalse(notificationDelivery.isAbandoned())
    }

    @Test
    fun `재시도 한도까지 실패하면 다음 시도 없이 발송을 포기함`() {
        /** given */
        val notificationDelivery = NotificationDelivery.of(notification, socialAccount, createdAt)
        val lastFailedAt = createdAt.plusHours(1)
        repeat(NotificationDelivery.MAX_RETRY_COUNT.toInt()) { notificationDelivery.scheduleRetry(createdAt) }

        /** when */
        notificationDelivery.scheduleRetry(lastFailedAt)

        /** then */
        assertTrue(notificationDelivery.isAbandoned())
        assertEquals(lastFailedAt, notificationDelivery.abandonedAt)
    }
}
