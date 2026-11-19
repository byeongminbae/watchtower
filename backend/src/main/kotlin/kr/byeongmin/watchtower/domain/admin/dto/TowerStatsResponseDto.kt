package kr.byeongmin.watchtower.domain.admin.dto

data class TowerStatsResponseDto(
    val totalCount: Long,
    val readyCount: Long,
    val errorCount: Long,
    val runningCount: Long,
    val pausedCount: Long,
    val suspendedCount: Long
)
