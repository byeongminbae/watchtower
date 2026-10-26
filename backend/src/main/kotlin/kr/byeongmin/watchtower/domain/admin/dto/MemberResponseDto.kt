package kr.byeongmin.watchtower.domain.admin.dto

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import java.time.LocalDateTime

/**
 * 어드민 전용 멤버 응답
 *
 * 멤버 패키지에 존재하는 응답보다 더 디테일함
 * @see [kr.byeongmin.watchtower.domain.member.dto.MemberResponseDto]
 * */
data class MemberResponseDto(
    val id: Long,
    val email: String,
    val nickname: String,
    val profileImageUrl: String,
    val role: MemberRole,
    val bannedAt: LocalDateTime?,
    val banReason: String?,
    val lastSignInAt: LocalDateTime,
    val deletedAt: LocalDateTime?
) {
    companion object {
        fun from(member: Member): MemberResponseDto {
            return MemberResponseDto(
                id = member.id.ifNullThrow(),
                email = member.email,
                nickname = member.nickname,
                profileImageUrl = member.profileImageUrl,
                role = member.role,
                bannedAt = member.bannedAt,
                banReason = member.banReason,
                lastSignInAt = member.lastSignInAt,
                deletedAt = member.deletedAt
            )
        }
    }
}
