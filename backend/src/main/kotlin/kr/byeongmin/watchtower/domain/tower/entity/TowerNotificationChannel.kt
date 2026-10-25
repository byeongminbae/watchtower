package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.domain.notification.entity.NotificationChannel

/**
 * 타워와 알림 채널의 다대다 관계를 풀어낸 연결 엔티티
 * @property [tower] 알림 채널을 사용할 타워
 * @property [notificationChannel] 타워에 연결된 알림 채널
 */
@Entity
@Table(name = "tower_notification_channel")
class TowerNotificationChannel private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tower_id")
    val tower: Tower,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_channel_id")
    val notificationChannel: NotificationChannel
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "tower_notification_channel_seq"
    )
    @SequenceGenerator(
        name = "tower_notification_channel_seq",
        sequenceName = "tower_notification_channel_id_seq",
        allocationSize = 30
    )
    override val id: Long? = null

    companion object {
        fun of(tower: Tower, notificationChannel: NotificationChannel): TowerNotificationChannel {
            val towerNotificationChannel = TowerNotificationChannel(
                tower = tower,
                notificationChannel = notificationChannel
            )
            tower.towerNotificationChannels.add(towerNotificationChannel)
            notificationChannel.towerNotificationChannels.add(towerNotificationChannel)
            return towerNotificationChannel
        }
    }
}
