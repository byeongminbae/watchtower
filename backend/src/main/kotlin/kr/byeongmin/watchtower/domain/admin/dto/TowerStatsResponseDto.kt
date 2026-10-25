package kr.byeongmin.watchtower.domain.admin.dto

data class TowerStatsResponseDto(
    val totalCount: Long,
    val runningCount: Long,
    val pausedCount: Long,
    val illegalSuspendedCount: Long
)
