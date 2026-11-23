package kr.byeongmin.watchtower.domain.notification.service

import kr.byeongmin.watchtower.domain.auth.accessor.SocialAccountAccessor
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
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class NotificationService(
    private val notificationAccessor: NotificationAccessor,
    private val notificationDeliveryAccessor: NotificationDeliveryAccessor,
    private val socialAccountAccessor: SocialAccountAccessor,
    private val socialAccountNotifier: SocialAccountNotifier,
    private val transactionTemplate: TransactionTemplate,
    private val eventPublisher: ApplicationEventPublisher,
    private val adminNotifier: AdminNotifier,
    private val timeProvider: TimeProvider
) {
    fun enqueueNotification(recipient: Member, content: String): Notification {
        val notification = notificationAccessor.save(Notification.of(recipient, content))
        val now = timeProvider.now()
        val notificationDeliveries = socialAccountAccessor.findSocialAccountsOfMember(recipient.id.ifNullThrow())
            .map { NotificationDelivery.of(notification, it, now) }

        notificationDeliveryAccessor.saveAll(notificationDeliveries)
        return notification
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    fun sendPendingDeliveries() {
        repeat(MAX_DELIVERIES_PER_RUN) {
            // 실패한것만 롤백하기 위해 limit 로 하나만 가져와서 처리함
            val isProcessed = transactionTemplate.execute { processNextDelivery() }.ifNullThrow()
            if (!isProcessed) {
                return
            }
        }
    }

    private fun processNextDelivery(): Boolean {
        val now = timeProvider.now()
        val notificationDelivery = notificationDeliveryAccessor.findFirstSendableDeliveryForUpdate(now) ?: return false
        val notification = notificationDelivery.notification

        try {
            notificationDelivery.socialAccount.sendNotification(socialAccountNotifier, notification.content)
        } catch (exception: Exception) {
            recordSendFailure(notificationDelivery, exception, now)
            return true
        }
        notificationDelivery.markSucceeded(now)
        eventPublisher.publishEvent(NotificationDeliverySucceededEvent(notification.id.ifNullThrow(), now))
        return true
    }

    private fun recordSendFailure(
        notificationDelivery: NotificationDelivery,
        exception: Exception,
        failedAt: LocalDateTime
    ) {
        if (exception is BusinessException && exception.error == NotificationError.RECIPIENT_UNREACHABLE) {
            notificationDelivery.markAbandoned(failedAt)
            return
        }

        notificationDelivery.scheduleRetry(failedAt)
        if (notificationDelivery.isAbandoned()) {
            adminNotifier.sendErrorAlert("알림 재시도 한도 초과 notificationDeliveryId=${notificationDelivery.id}", exception)
        }
    }

    companion object {
        const val MAX_DELIVERIES_PER_RUN = 20
    }
}
