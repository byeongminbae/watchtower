package kr.byeongmin.watchtower.domain.auth.entity

import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.entity.Base

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn
abstract class OAuth(
    @ManyToOne(fetch = FetchType.LAZY)
    protected val member: Member,
    protected val providerId: String
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