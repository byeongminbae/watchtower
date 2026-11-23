package kr.byeongmin.watchtower.domain.auth.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.auth.notifier.SocialAccountNotifier
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.entity.Base

/**
 * @property [providerId] 제공자측에서 고유하다고 보장하는 사용자 아이디
 * [sendNotification] Visitor 패턴으로 새로운 소셜로그인이 추가되도 [NotificationService] 를 수정하지 않도록 설계ㅎ함
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn
abstract class SocialAccount(
    @ManyToOne(fetch = FetchType.LAZY)
    val member: Member,
    val providerId: String
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "social_account_seq"
    )
    @SequenceGenerator(
        name = "social_account_seq",
        sequenceName = "social_account_id_seq",
        allocationSize = 30
    )
    override val id: Long? = null

    abstract fun sendNotification(notifier: SocialAccountNotifier, content: String)
}
