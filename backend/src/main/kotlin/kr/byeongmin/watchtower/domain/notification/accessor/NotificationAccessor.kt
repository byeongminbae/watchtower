package kr.byeongmin.watchtower.domain.notification.accessor

import kr.byeongmin.watchtower.domain.notification.entity.Notification
import kr.byeongmin.watchtower.domain.notification.repository.NotificationRepository
import org.springframework.stereotype.Component

@Component
class NotificationAccessor(
    private val notificationRepository: NotificationRepository
) {
    fun save(notification: Notification): Notification {
        return notificationRepository.save(notification)
    }
}
