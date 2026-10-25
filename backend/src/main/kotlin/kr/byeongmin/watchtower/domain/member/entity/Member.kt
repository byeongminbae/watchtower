package kr.byeongmin.watchtower.domain.member.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.domain.subscription.entity.Plan
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.global.entity.Base
import java.time.LocalDateTime

/**
 * @property [lastSignInAt] 가입하거나 로그인할 때 갱신됨
 * @property [refreshToken] 로그아웃 시 null 처리
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

    var lastSignInAt: LocalDateTime = lastSignInAt
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    var plan: Plan = plan
        protected set

    var refreshToken: String? = null
        protected set

    /** 리프레시 토큰을 재발급할때 사용 */
    fun rotateRefreshToken(refreshToken: String, now: LocalDateTime) {
        this.refreshToken = refreshToken
        this.lastSignInAt = now
    }

    /** 로그인 완료(토큰 발급) 후 호출 */
    fun completeSignIn(refreshToken: String, now: LocalDateTime) {
        this.refreshToken = refreshToken
        this.lastSignInAt = now
    }

    fun signOut() {
        this.refreshToken = null
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

    fun ban(now: LocalDateTime) {
        bannedAt = now
    }

    fun unban() {
        bannedAt = null
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
