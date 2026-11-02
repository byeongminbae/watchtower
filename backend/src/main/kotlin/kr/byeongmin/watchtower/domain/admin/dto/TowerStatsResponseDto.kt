package kr.byeongmin.watchtower.domain.admin.dto

data class TowerStatsResponseDto(
    val totalCount: Long,
    val crawlingReadyCount: Long,
    val crawlingErrorCount: Long,
    val crawlingCount: Long,
    val userPausedCount: Long,
    val adminPausedCount: Long
)
