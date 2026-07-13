package kr.byeongmin.watchtower.domain.admin.controller

import io.swagger.v3.oas.annotations.Operation
import kr.byeongmin.watchtower.global.response.SuccessResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/stats")
class StatsController {
    @Operation(summary = "대시보드 와치 통계 조회")
    @GetMapping("/watches")
    fun getWatchStats(): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "대시보드 결제 통계 조회")
    @GetMapping("/payments")
    fun getPaymentStats(): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "대시보드 유저수/회원탈퇴수 등 통계 조회")
    @GetMapping("/users")
    fun getUserStats(): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "대시보드 로그인/회원가입 통계 조회")
    @GetMapping("/auth")
    fun getAuthStats(): SuccessResponse<String> {
        return SuccessResponse("")
    }
}