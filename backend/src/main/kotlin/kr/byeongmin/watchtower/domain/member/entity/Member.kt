package kr.byeongmin.watchtower.domain.member.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.auth.dto.NaverProfileResponseExternalDto
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.utils.TimeUtil
import java.time.LocalDateTime

@Entity
class Member private constructor(
    val email: String,
    val nickname: String,
    val profileImageUrl: String,
    @Enumerated(EnumType.STRING)
    val role: MemberRole,
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

    var lastSignInAt: LocalDateTime = TimeUtil.entityTime()
        protected set

    var refreshToken: String? = null
        protected set

    fun rotateRefreshToken(refreshToken: String) {
        this.refreshToken = refreshToken
    }

    fun completeSignIn(refreshToken: String) {
        this.refreshToken = refreshToken
        this.lastSignInAt = TimeUtil.entityTime()
    }

    fun signOut() {
        this.refreshToken = null
    }

    companion object {
        fun from(naverMemberProfile: NaverProfileResponseExternalDto): Member {
            return Member(
                email = naverMemberProfile.response.email,
                nickname = naverMemberProfile.response.nickname,
                profileImageUrl = naverMemberProfile.response.profileImage,
                role = MemberRole.NORMAL
            )
        }
    }
}