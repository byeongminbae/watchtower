package kr.byeongmin.watchtower.domain.notification.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.entity.Base

@Entity
class Notification private constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    val recipient: Member,
    val content: String
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "notification_seq"
    )
    @SequenceGenerator(
        name = "notification_seq",
        sequenceName = "notification_id_seq",
        allocationSize = 30
    )
    override val id: Long? = null

    companion object {
        fun of(recipient: Member, content: String): Notification {
            return Notification(
                recipient = recipient,
                content = content
            )
        }
    }
}
