package kr.byeongmin.watchtower.domain.notification.service

import kr.byeongmin.watchtower.domain.auth.accessor.SocialAccountAccessor
import kr.byeongmin.watchtower.domain.auth.entity.SocialAccount
import kr.byeongmin.watchtower.domain.auth.notifier.SocialAccountNotifier
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.notification.accessor.NotificationAccessor
import kr.byeongmin.watchtower.domain.notification.accessor.NotificationDeliveryAccessor
import kr.byeongmin.watchtower.domain.notification.entity.Notification
import kr.byeongmin.watchtower.domain.notification.entity.NotificationDelivery
import kr.byeongmin.watchtower.domain.notification.error.NotificationError
import kr.byeongmin.watchtower.domain.notification.event.NotificationDeliverySucceededEvent
import kr.byeongmin.watchtower.global.alert.AdminNotifier
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NotificationServiceTest {
    private val notificationAccessor = mock<NotificationAccessor>()
    private val notificationDeliveryAccessor = mock<NotificationDeliveryAccessor>()
    private val socialAccountAccessor = mock<SocialAccountAccessor>()
    private val socialAccountNotifier = mock<SocialAccountNotifier>()
    private val eventPublisher = mock<ApplicationEventPublisher>()
    private val adminNotifier = mock<AdminNotifier>()
    private val transactionManager = mock<PlatformTransactionManager> {
        on { getTransaction(any()) } doReturn mock<TransactionStatus>()
    }
    private val now = LocalDateTime.of(2026, 11, 21, 3, 0)
    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn now
    }
    private val notificationService = NotificationService(
        notificationAccessor = notificationAccessor,
        notificationDeliveryAccessor = notificationDeliveryAccessor,
        socialAccountAccessor = socialAccountAccessor,
        socialAccountNotifier = socialAccountNotifier,
        transactionTemplate = TransactionTemplate(transactionManager),
        eventPublisher = eventPublisher,
        adminNotifier = adminNotifier,
        timeProvider = timeProvider
    )

    private val recipientId = 21L
    private val recipient = mock<Member> {
        on { id } doReturn recipientId
    }
    private val notificationId = 5L
    private val content = "알림 내용"
    private val notification = spy(Notification.of(recipient, content)) {
        on { id } doReturn notificationId
    }
    private val socialAccount = mock<SocialAccount>()
    private val notificationDelivery = spy(NotificationDelivery.of(notification, socialAccount, now)) {
        on { id } doReturn 8L
    }

    private fun stubSendableUntilProcessed() {
        whenever(notificationDeliveryAccessor.findFirstSendableDeliveryForUpdate(now)).thenAnswer {
            if (notificationDelivery.succeededAt == null && !notificationDelivery.isAbandoned()) notificationDelivery else null
        }
    }

    @Test
    fun `알림을 요청하면 알림을 저장하고 받는 회원의 계정마다 바로 보낼 발송을 만든 뒤 저장한 알림을 돌려줌`() {
        /** given */
        val firstSocialAccount = mock<SocialAccount>()
        val secondSocialAccount = mock<SocialAccount>()
        whenever(notificationAccessor.save(any<Notification>())).thenReturn(notification)
        whenever(socialAccountAccessor.findSocialAccountsOfMember(recipientId))
            .thenReturn(listOf(firstSocialAccount, secondSocialAccount))

        /** when */
        val result = notificationService.enqueueNotification(recipient, content)

        /** then */
        assertSame(notification, result)
        val savedNotification = argumentCaptor<Notification>()
        verify(notificationAccessor).save(savedNotification.capture())
        assertSame(recipient, savedNotification.firstValue.recipient)
        assertEquals(content, savedNotification.firstValue.content)
        val savedDeliveries = argumentCaptor<List<NotificationDelivery>>()
        verify(notificationDeliveryAccessor).saveAll(savedDeliveries.capture())
        assertEquals(listOf(firstSocialAccount, secondSocialAccount), savedDeliveries.firstValue.map { it.socialAccount })
        assertTrue(savedDeliveries.firstValue.all { it.notification === notification && it.nextAttemptAt == now })
        verifyNoInteractions(socialAccountNotifier)
    }

    @Test
    fun `연결된 계정이 없는 회원의 알림은 발송 없이 알림만 저장함`() {
        /** given */
        whenever(notificationAccessor.save(any<Notification>())).thenReturn(notification)
        whenever(socialAccountAccessor.findSocialAccountsOfMember(recipientId)).thenReturn(emptyList())

        /** when */
        val result = notificationService.enqueueNotification(recipient, content)

        /** then */
        assertSame(notification, result)
        verify(notificationDeliveryAccessor).saveAll(emptyList())
    }

    @Test
    fun `보낼 발송은 계정에 맞는 방법으로 내용을 보내고 발송 성공 시각을 남긴 뒤 발송 성공 이벤트를 발행함`() {
        /** given */
        stubSendableUntilProcessed()

        /** when */
        notificationService.sendPendingDeliveries()

        /** then */
        verify(socialAccount).sendNotification(socialAccountNotifier, content)
        assertEquals(now, notificationDelivery.succeededAt)
        verify(eventPublisher).publishEvent(NotificationDeliverySucceededEvent(notificationId, now))
    }

    @Test
    fun `보낼 발송이 없으면 아무것도 보내지 않음`() {
        /** given */
        whenever(notificationDeliveryAccessor.findFirstSendableDeliveryForUpdate(now)).thenReturn(null)

        /** when */
        notificationService.sendPendingDeliveries()

        /** then */
        verifyNoInteractions(socialAccount, eventPublisher)
    }

    @Test
    fun `한 번 실행할 때 정해진 개수까지만 발송함`() {
        /** given */
        whenever(notificationDeliveryAccessor.findFirstSendableDeliveryForUpdate(now)).thenReturn(notificationDelivery)

        /** when */
        notificationService.sendPendingDeliveries()

        /** then */
        verify(socialAccount, times(NotificationService.MAX_DELIVERIES_PER_RUN)).sendNotification(socialAccountNotifier, content)
    }

    @Test
    fun `알림을 받을 수 없는 계정의 발송은 재시도하지 않고 포기함`() {
        /** given */
        stubSendableUntilProcessed()
        doThrow(BusinessException(NotificationError.RECIPIENT_UNREACHABLE))
            .whenever(socialAccount).sendNotification(socialAccountNotifier, content)

        /** when */
        notificationService.sendPendingDeliveries()

        /** then */
        assertEquals(now, notificationDelivery.abandonedAt)
        assertEquals(0, notificationDelivery.failedAttemptCount)
        verifyNoInteractions(adminNotifier, eventPublisher)
    }

    @Test
    fun `일시적인 오류로 보내지 못한 발송은 지수 백오프로 계산한 시각에 다시 보냄`() {
        /** given */
        whenever(notificationDeliveryAccessor.findFirstSendableDeliveryForUpdate(now)).thenReturn(notificationDelivery, null)
        doThrow(BusinessException(CommonError.EXTERNAL_API_ERROR))
            .whenever(socialAccount).sendNotification(socialAccountNotifier, content)

        /** when */
        notificationService.sendPendingDeliveries()

        /** then */
        assertEquals(1, notificationDelivery.failedAttemptCount)
        assertTrue(
            notificationDelivery.nextAttemptAt in
                now.plus(NotificationDelivery.RETRY_INITIAL_INTERVAL)..now.plus(NotificationDelivery.RETRY_INITIAL_INTERVAL).plus(NotificationDelivery.RETRY_JITTER)
        )
        assertNull(notificationDelivery.succeededAt)
        assertNull(notificationDelivery.abandonedAt)
        verifyNoInteractions(adminNotifier, eventPublisher)
    }

    @Test
    fun `재시도 한도를 넘겨도 보내지 못한 발송은 포기하고 운영자에게 알림`() {
        /** given */
        stubSendableUntilProcessed()
        val exception = BusinessException(CommonError.EXTERNAL_API_ERROR)
        doThrow(exception).whenever(socialAccount).sendNotification(socialAccountNotifier, content)

        /** when */
        notificationService.sendPendingDeliveries()

        /** then */
        assertTrue(notificationDelivery.isAbandoned())
        assertEquals(NotificationDelivery.MAX_RETRY_COUNT.toInt() + 1, notificationDelivery.failedAttemptCount)
        verify(socialAccount, times(NotificationDelivery.MAX_RETRY_COUNT.toInt() + 1))
            .sendNotification(socialAccountNotifier, content)
        verify(adminNotifier).sendErrorAlert(any(), eq(exception))
    }
}
