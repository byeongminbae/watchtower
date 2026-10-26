package kr.byeongmin.watchtower.domain.member.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.domain.subscription.entity.Plan
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.utils.isNotNull
import java.time.LocalDateTime

/**
 * @property [lastSignInAt] 가입하거나 로그인할 때 갱신됨
 */
@Entity
class Member private constructor(
    val email: String,
    nickname: String,
    profileImageUrl: String,
    role: MemberRole,
    lastSignInAt: LocalDateTime,
    plan: Plan
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
    override val id: Long? = null

    var nickname: String = nickname
        protected set

    var profileImageUrl: String = profileImageUrl
        protected set

    @Enumerated(EnumType.STRING)
    var role: MemberRole = role
        protected set

    var bannedAt: LocalDateTime? = null
        protected set

    var banReason: String? = null
        protected set

    var lastSignInAt: LocalDateTime = lastSignInAt
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    var plan: Plan = plan
        protected set

    fun ensureSignInAllowed() {
        if (isDeleted()) {
            throw BusinessException(AuthError.ALREADY_DELETED_ACCOUNT)
        }
        if (bannedAt.isNotNull()) {
            throw BusinessException(AuthError.BANNED_ACCOUNT)
        }
    }

    fun completeSignIn(now: LocalDateTime) {
        this.lastSignInAt = now
    }

    fun updateProfile(nickname: String, profileImageUrl: String) {
        this.nickname = nickname
        this.profileImageUrl = profileImageUrl
    }

    fun changeRole(role: MemberRole) {
        this.role = role
    }

    fun isTowerAddable(currentTowerQuantity: Long): Boolean {
        return currentTowerQuantity < plan.maxTowerQuantity
    }

    fun ban(now: LocalDateTime, banReason: String) {
        bannedAt = now
        this.banReason = banReason
    }

    fun unban() {
        bannedAt = null
        banReason = null
    }

    companion object {
        fun from(
            naverProfileResponseDto: NaverProfileResponseDto,
            now: LocalDateTime,
            plan: Plan
        ): Member {
            return Member(
                email = naverProfileResponseDto.response.email,
                nickname = naverProfileResponseDto.response.nickname,
                profileImageUrl = naverProfileResponseDto.response.profileImage,
                role = MemberRole.NORMAL,
                lastSignInAt = now,
                plan = plan
            )
        }
    }
}
