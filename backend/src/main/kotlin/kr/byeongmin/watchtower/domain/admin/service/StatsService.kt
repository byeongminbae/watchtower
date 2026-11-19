package kr.byeongmin.watchtower.domain.admin.service

import kr.byeongmin.watchtower.domain.admin.dto.AuthStatsResponseDto
import kr.byeongmin.watchtower.domain.admin.dto.AuthStatsResponseDto.PeriodStats
import kr.byeongmin.watchtower.domain.admin.dto.MemberStatsResponseDto
import kr.byeongmin.watchtower.domain.admin.dto.TowerStatsResponseDto
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class StatsService(
    private val towerAccessor: TowerAccessor,
    private val memberAccessor: MemberAccessor,
    private val timeProvider: TimeProvider
) {
    fun getTowerStats(): SuccessDataResponse<TowerStatsResponseDto> {
        val countByStatus = TowerStatus.entries.associateWith {
            towerAccessor.countTowers(
                status = it
            )
        }

        return SuccessDataResponse(
            TowerStatsResponseDto(
                totalCount = countByStatus.values.sum(),
                readyCount = countByStatus.getValue(TowerStatus.READY),
                errorCount = countByStatus.getValue(TowerStatus.ERROR),
                runningCount = countByStatus.getValue(TowerStatus.RUNNING),
                pausedCount = countByStatus.getValue(TowerStatus.PAUSED),
                suspendedCount = countByStatus.getValue(TowerStatus.SUSPENDED)
            )
        )
    }

    fun getMemberStats(): SuccessDataResponse<MemberStatsResponseDto> {
        return SuccessDataResponse(
            MemberStatsResponseDto(
                totalCount = memberAccessor.countMembers(),
                activeCount = memberAccessor.countActiveMembers(),
                bannedCount = memberAccessor.countBannedMembers(),
                deletedCount = memberAccessor.countDeletedMembers()
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
                    last7Days = memberAccessor.countSignedUpMembers(
                        signedUpAfter = now.minus(last7Days)
                    ),
                    last30Days = memberAccessor.countSignedUpMembers(
                        signedUpAfter = now.minus(last30Days)
                    )
                ),
                signIns = PeriodStats(
                    last7Days = memberAccessor.countSignedInMembers(
                        signedInAfter = now.minus(last7Days)
                    ),
                    last30Days = memberAccessor.countSignedInMembers(
                        signedInAfter = now.minus(last30Days)
                    )
                )
            )
        )
    }
}
