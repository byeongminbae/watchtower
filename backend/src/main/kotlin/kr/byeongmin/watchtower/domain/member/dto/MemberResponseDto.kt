package kr.byeongmin.watchtower.domain.member.dto

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.utils.TimeUtil
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import java.time.LocalDateTime

data class MemberResponseDto(
    val id: Long,
    val email: String,
    val nickname: String,
    val profileImageUrl: String,
    val lastSignInAt: LocalDateTime = TimeUtil.entityTime(),
) {
    companion object {
        fun from(member: Member): MemberResponseDto {
            return MemberResponseDto(
                id = member.id.ifNullThrow(),
                email = member.email,
                nickname = member.nickname,
                profileImageUrl = member.profileImageUrl,
                lastSignInAt = member.lastSignInAt
            )
        }
    }
}