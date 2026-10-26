package kr.byeongmin.watchtower.domain.admin.dto

data class MemberStatusUpdateRequestDto(
    val banned: Boolean,
    val banReason: String
)
