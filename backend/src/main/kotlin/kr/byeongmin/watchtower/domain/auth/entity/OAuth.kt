package kr.byeongmin.watchtower.domain.auth.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.entity.Base

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn
abstract class OAuth(
    @ManyToOne(fetch = FetchType.LAZY)
    val member: Member,
    val providerId: String
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "member_oauth_seq"
    )
    @SequenceGenerator(
        name = "member_oauth_seq",
        sequenceName = "member_oauth_id_seq",
        allocationSize = 30
    )
    val id: Long? = null
}