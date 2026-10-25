package kr.byeongmin.watchtower.domain.admin.controller

import kr.byeongmin.watchtower.domain.admin.dto.AuthStatsResponseDto
import kr.byeongmin.watchtower.domain.admin.dto.AuthStatsResponseDto.PeriodStats
import kr.byeongmin.watchtower.domain.admin.dto.MemberStatsResponseDto
import kr.byeongmin.watchtower.domain.admin.dto.TowerStatsResponseDto
import kr.byeongmin.watchtower.domain.admin.service.StatsService
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class StatsControllerTest {
    private val service = mock<StatsService>()
    private val mvc = MockMvcBuilders.standaloneSetup(StatsController(service)).build()

    @Test
    fun `대시보드에서 전체 타워 수와 상태별 타워 수를 확인할 수 있음`() {
        /** given */
        whenever(service.getTowerStats()).thenReturn(
            SuccessDataResponse(
                TowerStatsResponseDto(
                    totalCount = 9,
                    runningCount = 5,
                    pausedCount = 3,
                    illegalSuspendedCount = 1
                )
            )
        )
        /** when */
        val result = mvc
            .perform(get("/api/v1/admin/stats/towers"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalCount").value(9))
            .andExpect(jsonPath("$.data.runningCount").value(5))
            .andExpect(jsonPath("$.data.pausedCount").value(3))
            .andExpect(jsonPath("$.data.illegalSuspendedCount").value(1))
    }

    @Test
    fun `대시보드에서 전체 회원 수와 상태별 회원 수를 확인할 수 있음`() {
        /** given */
        whenever(service.getMemberStats()).thenReturn(
            SuccessDataResponse(
                MemberStatsResponseDto(
                    totalCount = 10,
                    activeCount = 8,
                    bannedCount = 2,
                    deletedCount = 4
                )
            )
        )

        /** when */
        val result = mvc
            .perform(get("/api/v1/admin/stats/members"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalCount").value(10))
            .andExpect(jsonPath("$.data.activeCount").value(8))
            .andExpect(jsonPath("$.data.bannedCount").value(2))
            .andExpect(jsonPath("$.data.deletedCount").value(4))
    }

    @Test
    fun `대시보드에서 최근 7일과 30일의 가입 및 로그인 수를 확인할 수 있음`() {
        /** given */
        whenever(service.getAuthStats()).thenReturn(
            SuccessDataResponse(
                AuthStatsResponseDto(
                    signUps = PeriodStats(last7Days = 3, last30Days = 12),
                    signIns = PeriodStats(last7Days = 7, last30Days = 14)
                )
            )
        )

        /** when */
        val result = mvc
            .perform(get("/api/v1/admin/stats/auth"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.signUps.last7Days").value(3))
            .andExpect(jsonPath("$.data.signUps.last30Days").value(12))
            .andExpect(jsonPath("$.data.signIns.last7Days").value(7))
            .andExpect(jsonPath("$.data.signIns.last30Days").value(14))
    }
}
