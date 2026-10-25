package kr.byeongmin.watchtower.domain.admin.dto

data class AuthStatsResponseDto(
    val signUps: PeriodStats,
    val signIns: PeriodStats
) {
    data class PeriodStats(
        val last7Days: Long,
        val last30Days: Long
    )
}
