package kr.byeongmin.watchtower.domain.member.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.global.entity.Base
import java.time.LocalDateTime

@Entity
class Member(
    private val role: MemberRole = MemberRole.USER,
    private val lastLoginAt: LocalDateTime,
    private val nickname: String,
    private val email: String,
    private val profileImageUrl: String
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "member_seq"
    )
    @SequenceGenerator(
        name = "member_seq",
        sequenceName = "member_id_seq",
        allocationSize = 30
    )
    val id: Long? = null
}