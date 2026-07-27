package kr.byeongmin.watchtower.domain.member.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.utils.TimeUtil
import java.time.LocalDateTime

@Entity
class Member(
    private val email: String,
    private val nickname: String,
    private val profileImageUrl: String,
    private val lastLoginAt: LocalDateTime = TimeUtil.entityTime(),
    @Enumerated(EnumType.STRING)
    val role: MemberRole = MemberRole.USER
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