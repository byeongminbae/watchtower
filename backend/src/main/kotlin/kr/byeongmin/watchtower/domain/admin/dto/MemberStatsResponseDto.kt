package kr.byeongmin.watchtower.domain.admin.dto

data class MemberStatsResponseDto(
    val totalCount: Long,
    val activeCount: Long,
    val bannedCount: Long,
    val deletedCount: Long
)
