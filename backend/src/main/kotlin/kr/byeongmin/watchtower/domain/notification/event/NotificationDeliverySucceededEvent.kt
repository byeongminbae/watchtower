package kr.byeongmin.watchtower.domain.notification.event

import java.time.LocalDateTime

data class NotificationDeliverySucceededEvent(
    val notificationId: Long,
    val succeededAt: LocalDateTime
)
