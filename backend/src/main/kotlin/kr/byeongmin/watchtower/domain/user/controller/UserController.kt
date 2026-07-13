package kr.byeongmin.watchtower.domain.user.controller

import io.swagger.v3.oas.annotations.Operation
import kr.byeongmin.watchtower.global.response.SuccessResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController {
    @Operation(summary = "프로필 조회")
    @GetMapping("/{userId}")
    fun getUser(
        @PathVariable userId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "결제 이력 조회")
    @GetMapping("/{userId}/payments")
    fun getUserPaymentHistories(
        @PathVariable userId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "와치 목록 조회")
    @GetMapping("/{userId}/watches")
    fun getUserWatches(
        @PathVariable userId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "프로필 정보 수정")
    @PatchMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "회원 탈퇴 (논리삭제 + 네이버 revoke)")
    @DeleteMapping("/{userId}")
    fun deleteUser(
        @PathVariable userId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "현재 구독중인 플랜 조회")
    @GetMapping("/{userId}/subscriptions")
    fun getUserSubscription(
        @PathVariable userId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "구독 해지 요청(환불)")
    @DeleteMapping("/{userId}/subscriptions")
    fun cancelUserSubscription(
        @PathVariable userId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }
}