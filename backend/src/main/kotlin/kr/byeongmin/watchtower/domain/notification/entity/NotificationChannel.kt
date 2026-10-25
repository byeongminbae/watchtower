package kr.byeongmin.watchtower.domain.notification.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.notification.enums.NotificationChannelType
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.tower.entity.TowerNotificationChannel
import kr.byeongmin.watchtower.global.entity.Base

/**
 * 회원이 알림을 받을 채널
 * @property [name] 유저가 지정한 채널의 이름. null 일 경우 [type] 이름이 소문자로 등록됨
 * @property [type] 디스코드, 텔레그램 등 채널의 타입
 */
@Entity
class NotificationChannel private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    val member: Member,
    name: String,
    @Enumerated(EnumType.STRING)
    val type: NotificationChannelType,
    webhookUrl: String
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "notification_channel_seq"
    )
    @SequenceGenerator(
        name = "notification_channel_seq",
        sequenceName = "notification_channel_id_seq",
        allocationSize = 30
    )
    override val id: Long? = null

    var name: String = name
        protected set

    var webhookUrl: String = webhookUrl
        protected set

    fun update(name: String, webhookUrl: String) {
        this.name = name
        this.webhookUrl = webhookUrl
    }

    @OneToMany(mappedBy = "notificationChannel", fetch = FetchType.LAZY)
    val towerNotificationChannels: MutableList<TowerNotificationChannel> = mutableListOf()

    companion object {
        fun from(
            member: Member,
            name: String?,
            type: NotificationChannelType,
            webhookUrl: String
        ): NotificationChannel {
            return NotificationChannel(
                member = member,
                name = name ?: type.name.lowercase(),
                type = type,
                webhookUrl = webhookUrl
            )
        }
    }
}
