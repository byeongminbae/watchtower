package kr.byeongmin.watchtower.domain.member.controller

import io.swagger.v3.oas.annotations.Operation
import kr.byeongmin.watchtower.global.response.SuccessResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/member")
class MemberController {
    @Operation(summary = "프로필 조회")
    @GetMapping("/{memberId}")
    fun getMember(
        @PathVariable memberId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "결제 이력 조회")
    @GetMapping("/{memberId}/payments")
    fun getMemberPaymentHistories(
        @PathVariable memberId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "와치 목록 조회")
    @GetMapping("/{memberId}/watches")
    fun getMemberWatches(
        @PathVariable memberId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "프로필 정보 수정")
    @PatchMapping("/{memberId}")
    fun updateMember(
        @PathVariable memberId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "회원 탈퇴 (논리삭제 + 네이버 revoke)")
    @DeleteMapping("/{memberId}")
    fun deleteMember(
        @PathVariable memberId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "현재 구독중인 플랜 조회")
    @GetMapping("/{memberId}/subscriptions")
    fun getMemberSubscription(
        @PathVariable memberId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "구독 해지 요청(환불)")
    @DeleteMapping("/{memberId}/subscriptions")
    fun cancelMemberSubscription(
        @PathVariable memberId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }
}