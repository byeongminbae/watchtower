package kr.byeongmin.watchtower.domain.member.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import kr.byeongmin.watchtower.domain.member.dto.MemberResponseDto
import kr.byeongmin.watchtower.domain.member.dto.MemberUpdateRequestDto
import kr.byeongmin.watchtower.domain.member.service.MemberService
import kr.byeongmin.watchtower.domain.tower.dto.TowerListItemResponseDto
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.CursorPaginationResponseDto
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.security.OwnerOnly
import org.springdoc.core.annotations.ParameterObject
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

    @Operation(summary = "타워 목록 조회 [커서]")
    @GetMapping("/{memberId}/towers")
    @OwnerOnly
    fun getMemberTowers(
        @PathVariable memberId: Long,
        @Valid @ParameterObject pagination: CursorPaginationRequestDto,
        @RequestParam(defaultValue = "ID") sortField: BaseSortField
    ): SuccessDataResponse<CursorPaginationResponseDto<TowerListItemResponseDto>> {
        return memberService.getMemberTowers(
            memberId = memberId,
            cursorPaginationRequestDto = pagination,
            sortField = sortField
        )
    }

    @Operation(summary = "프로필 정보 수정")
    @PutMapping("/{memberId}")
    @OwnerOnly
    fun updateMember(
        @PathVariable memberId: Long,
        @RequestBody memberUpdateRequestDto: MemberUpdateRequestDto
    ): SuccessDataResponse<MemberResponseDto> {
        return memberService.updateMember(
            memberId = memberId,
            memberUpdateRequestDto = memberUpdateRequestDto
        )
    }

    @Operation(summary = "회원 탈퇴 (회원과 소유 리소스 논리 삭제)")
    @DeleteMapping("/{memberId}")
    @OwnerOnly
    fun deleteMember(
        @PathVariable memberId: Long
    ): SuccessResponse {
        return memberService.deleteMember(memberId)
    }
}
