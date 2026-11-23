package kr.byeongmin.watchtower.domain.notification.accessor

import kr.byeongmin.watchtower.domain.notification.entity.NotificationDelivery
import kr.byeongmin.watchtower.domain.notification.repository.NotificationDeliveryQuerydslRepository
import kr.byeongmin.watchtower.domain.notification.repository.NotificationDeliveryRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NotificationDeliveryAccessor(
    private val notificationDeliveryRepository: NotificationDeliveryRepository,
    private val notificationDeliveryQuerydslRepository: NotificationDeliveryQuerydslRepository
) {
    fun findFirstSendableDeliveryForUpdate(now: LocalDateTime): NotificationDelivery? {
        return notificationDeliveryQuerydslRepository.findFirstSendableForUpdate(now)
    }

    fun saveAll(notificationDeliveries: List<NotificationDelivery>): List<NotificationDelivery> {
        return notificationDeliveryRepository.saveAll(notificationDeliveries)
    }
}
