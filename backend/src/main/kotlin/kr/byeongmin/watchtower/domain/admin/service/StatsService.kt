package kr.byeongmin.watchtower.domain.admin.service

import java.time.Duration
import kr.byeongmin.watchtower.domain.admin.dto.AuthStatsResponseDto
import kr.byeongmin.watchtower.domain.admin.dto.AuthStatsResponseDto.PeriodStats
import kr.byeongmin.watchtower.domain.admin.dto.MemberStatsResponseDto
import kr.byeongmin.watchtower.domain.admin.dto.TowerStatsResponseDto
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.config.TimeProvider
import org.springframework.stereotype.Service

@Service
class StatsService(
    private val towerAccessor: TowerAccessor,
    private val memberAccessor: MemberAccessor,
    private val timeProvider: TimeProvider
) {
    fun getTowerStats(): SuccessDataResponse<TowerStatsResponseDto> {
        val countByStatus = TowerStatus.entries.associateWith { towerAccessor.countByStatus(it) }

        return SuccessDataResponse(
            TowerStatsResponseDto(
                totalCount = countByStatus.values.sum(),
                crawlingReadyCount = countByStatus.getValue(TowerStatus.CRAWLING_READY),
                crawlingErrorCount = countByStatus.getValue(TowerStatus.CRAWLING_ERROR),
                crawlingCount = countByStatus.getValue(TowerStatus.CRAWLING),
                userPausedCount = countByStatus.getValue(TowerStatus.USER_PAUSED),
                adminPausedCount = countByStatus.getValue(TowerStatus.ADMIN_PAUSED)
            )
        )
    }

    fun getMemberStats(): SuccessDataResponse<MemberStatsResponseDto> {
        return SuccessDataResponse(
            MemberStatsResponseDto(
                totalCount = memberAccessor.count(),
                activeCount = memberAccessor.countActive(),
                bannedCount = memberAccessor.countBanned(),
                deletedCount = memberAccessor.countDeleted()
            )
        )
    }

    /** 일정 기간안에 계정 생성, 로그인 한 유저들의 수 */
    fun getAuthStats(): SuccessDataResponse<AuthStatsResponseDto> {
        val now = timeProvider.now()
        val last7Days = Duration.ofDays(7)
        val last30Days = Duration.ofDays(30)

        return SuccessDataResponse(
            AuthStatsResponseDto(
                signUps = PeriodStats(
                    last7Days = memberAccessor.countByCreatedAtAfter(now.minus(last7Days)),
                    last30Days = memberAccessor.countByCreatedAtAfter(now.minus(last30Days))
                ),
                signIns = PeriodStats(
                    last7Days = memberAccessor.countByLastSignInAtAfter(now.minus(last7Days)),
                    last30Days = memberAccessor.countByLastSignInAtAfter(now.minus(last30Days))
                )
            )
        )
    }
}
