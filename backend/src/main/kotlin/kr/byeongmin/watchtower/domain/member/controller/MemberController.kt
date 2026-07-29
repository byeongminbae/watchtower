package kr.byeongmin.watchtower.domain.member.controller

import io.swagger.v3.oas.annotations.Operation
import kr.byeongmin.watchtower.domain.member.dto.MemberResponseDto
import kr.byeongmin.watchtower.domain.member.service.MemberService
import kr.byeongmin.watchtower.global.response.SuccessDataResponse
import kr.byeongmin.watchtower.global.response.SuccessResponse
import kr.byeongmin.watchtower.global.security.OwnerOnly
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/member")
class MemberController(
    private val memberService: MemberService
) {
    @Operation(summary = "프로필 조회")
    @GetMapping("/{memberId}")
    @OwnerOnly
    fun getMember(
        @PathVariable memberId: Long
    ): SuccessDataResponse<MemberResponseDto> {
        return memberService.getMember(memberId)
    }

    @Operation(summary = "결제 이력 조회")
    @GetMapping("/{memberId}/payments")
    @OwnerOnly
    fun getMemberPaymentHistories(
        @PathVariable memberId: Long
    ): SuccessDataResponse<String> {
        return SuccessDataResponse("")
    }

    @Operation(summary = "와치 목록 조회")
    @GetMapping("/{memberId}/watches")
    @OwnerOnly
    fun getMemberWatches(
        @PathVariable memberId: Long
    ): SuccessDataResponse<String> {
        return SuccessDataResponse("")
    }

    @Operation(summary = "프로필 정보 수정")
    @PatchMapping("/{memberId}")
    @OwnerOnly
    fun updateMember(
        @PathVariable memberId: Long
    ): SuccessDataResponse<String> {
        return SuccessDataResponse("")
    }

    @Operation(summary = "회원 탈퇴 (논리삭제 + 네이버 revoke)")
    @DeleteMapping("/{memberId}")
    @OwnerOnly
    fun deleteMember(
        @PathVariable memberId: Long
    ): SuccessResponse {
        return SuccessResponse()
    }

    @Operation(summary = "현재 구독중인 플랜 조회")
    @GetMapping("/{memberId}/subscriptions")
    @OwnerOnly
    fun getMemberSubscription(
        @PathVariable memberId: Long
    ): SuccessDataResponse<String> {
        return SuccessDataResponse("")
    }

    @Operation(summary = "구독 해지 요청(환불)")
    @DeleteMapping("/{memberId}/subscriptions")
    @OwnerOnly
    fun cancelMemberSubscription(
        @PathVariable memberId: Long
    ): SuccessResponse {
        return SuccessResponse()
    }
}
