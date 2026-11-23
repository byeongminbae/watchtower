package kr.byeongmin.watchtower.domain.tower.listener

import kr.byeongmin.watchtower.domain.notification.event.NotificationDeliverySucceededEvent
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

class TowerSnapshotNotificationListenerTest {
    private val towerSnapshotAccessor = mock<TowerSnapshotAccessor>()
    private val towerSnapshotNotificationListener = TowerSnapshotNotificationListener(towerSnapshotAccessor)
    private val notificationId = 31L
    private val succeededAt = LocalDateTime.of(2026, 11, 21, 3, 0, 5)

    @Test
    fun `변경 알림을 보내는 데 성공하면 그 알림을 요청한 스냅샷에 알림 시각을 남김`() {
        /** given */
        val towerSnapshot = mock<TowerSnapshot>()
        whenever(towerSnapshotAccessor.findTowerSnapshotOfNotification(notificationId)).thenReturn(towerSnapshot)

        /** when */
        towerSnapshotNotificationListener.markTowerSnapshotNotified(NotificationDeliverySucceededEvent(notificationId, succeededAt))

        /** then */
        verify(towerSnapshot).markNotified(succeededAt)
    }
}
