package kr.byeongmin.watchtower.domain.member.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.domain.subscription.entity.Plan
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
    var nickname: String,
    var profileImageUrl: String,
    @Enumerated(EnumType.STRING)
    var role: MemberRole,
    var lastSignInAt: LocalDateTime,
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

    var bannedAt: LocalDateTime? = null
        protected set

    var banReason: String? = null
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
        fun of(
            email: String,
            nickname: String,
            profileImageUrl: String,
            lastSignInAt: LocalDateTime,
            plan: Plan
        ): Member {
            return Member(
                email = email,
                nickname = nickname,
                profileImageUrl = profileImageUrl,
                role = MemberRole.NORMAL,
                lastSignInAt = lastSignInAt,
                plan = plan
            )
        }
    }
}
