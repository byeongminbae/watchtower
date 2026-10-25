package kr.byeongmin.watchtower.domain.admin.controller

import io.swagger.v3.oas.annotations.Operation
import kr.byeongmin.watchtower.domain.admin.dto.AuthStatsResponseDto
import kr.byeongmin.watchtower.domain.admin.dto.MemberStatsResponseDto
import kr.byeongmin.watchtower.domain.admin.dto.TowerStatsResponseDto
import kr.byeongmin.watchtower.domain.admin.service.StatsService
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.security.AdminOnly
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/stats")
@AdminOnly
class StatsController(
    private val statsService: StatsService
) {
    @Operation(summary = "대시보드 타워 통계 조회")
    @GetMapping("/towers")
    fun getTowerStats(): SuccessDataResponse<TowerStatsResponseDto> {
        return statsService.getTowerStats()
    }

    @Operation(summary = "대시보드 회원수/회원탈퇴수 등 통계 조회")
    @GetMapping("/members")
    fun getMemberStats(): SuccessDataResponse<MemberStatsResponseDto> {
        return statsService.getMemberStats()
    }

    @Operation(summary = "대시보드 로그인/회원가입 통계 조회")
    @GetMapping("/auth")
    fun getAuthStats(): SuccessDataResponse<AuthStatsResponseDto> {
        return statsService.getAuthStats()
    }
}
