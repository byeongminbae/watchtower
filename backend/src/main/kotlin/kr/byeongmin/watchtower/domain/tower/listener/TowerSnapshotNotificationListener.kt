package kr.byeongmin.watchtower.domain.tower.listener

import kr.byeongmin.watchtower.domain.notification.event.NotificationDeliverySucceededEvent
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TowerSnapshotNotificationListener(
    private val towerSnapshotAccessor: TowerSnapshotAccessor
) {
    @EventListener
    fun markTowerSnapshotNotified(event: NotificationDeliverySucceededEvent) {
        val towerSnapshot = towerSnapshotAccessor.findTowerSnapshotOfNotification(event.notificationId) ?: return
        towerSnapshot.markNotified(event.succeededAt)
    }
}
