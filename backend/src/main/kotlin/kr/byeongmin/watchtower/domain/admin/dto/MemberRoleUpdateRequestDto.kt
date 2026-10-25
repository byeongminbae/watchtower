package kr.byeongmin.watchtower.domain.admin.dto

import kr.byeongmin.watchtower.domain.member.enums.MemberRole

data class MemberRoleUpdateRequestDto(
    val role: MemberRole
)
