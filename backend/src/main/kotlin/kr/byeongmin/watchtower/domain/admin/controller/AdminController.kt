package kr.byeongmin.watchtower.domain.admin.controller

import io.swagger.v3.oas.annotations.Operation
import kr.byeongmin.watchtower.global.response.SuccessResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
class AdminController {
    @Operation(summary = "와치 상태 변경(정책 위반 정지 포함)")
    @PatchMapping("/watches/{watchId}")
    fun updateWatchStatus(
        @PathVariable watchId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "결제 직권 취소")
    @PostMapping("/payments/{paymentId}/cancel")
    fun cancelPayment(
        @PathVariable paymentId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "결제 내역 조회/검색")
    @GetMapping("/payments")
    fun getPayments(): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "전체 유저 조회/검색")
    @GetMapping("/users")
    fun getUsers(): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "유저 권한 수정")
    @PatchMapping("/users/{userId}/roles")
    fun updateUserRole(
        @PathVariable userId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "ban/unban 등 유저 상태 변경")
    @PatchMapping("/users/{userId}/status")
    fun updateUserStatus(
        @PathVariable userId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }
}