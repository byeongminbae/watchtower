package kr.byeongmin.watchtower.domain.auth.dto

data class MemberSessionResponseDto(
    val memberId: Long,
    val role: String
)
