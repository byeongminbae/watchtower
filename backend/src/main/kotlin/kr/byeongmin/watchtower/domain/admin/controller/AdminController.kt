package kr.byeongmin.watchtower.domain.admin.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import kr.byeongmin.watchtower.domain.admin.dto.MemberResponseDto
import kr.byeongmin.watchtower.domain.admin.dto.MemberRoleUpdateRequestDto
import kr.byeongmin.watchtower.domain.admin.dto.MemberStatusUpdateRequestDto
import kr.byeongmin.watchtower.domain.admin.dto.TowerStatusUpdateRequestDto
import kr.byeongmin.watchtower.domain.admin.service.AdminService
import kr.byeongmin.watchtower.domain.tower.dto.TowerResponseDto
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.OffsetPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.OffsetPaginationResponseDto
import kr.byeongmin.watchtower.global.security.AdminOnly
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin")
@AdminOnly
class AdminController(
    private val adminService: AdminService
) {
    @Operation(summary = "타워 상태 변경(정책 위반 정지 포함)")
    @PutMapping("/towers/{towerId}")
    fun updateTowerStatus(
        @PathVariable towerId: Long,
        @RequestBody towerStatusUpdateRequestDto: TowerStatusUpdateRequestDto
    ): SuccessDataResponse<TowerResponseDto> {
        return adminService.updateTowerStatus(
            towerId = towerId,
            towerStatusUpdateRequestDto = towerStatusUpdateRequestDto
        )
    }

    @Operation(summary = "전체 타워 목록 조회/검색 [오프셋]")
    @GetMapping("/towers")
    fun getTowers(
        @Valid @ParameterObject offsetPaginationRequestDto: OffsetPaginationRequestDto,
        @RequestParam(defaultValue = "ID") sortField: BaseSortField
    ): SuccessDataResponse<OffsetPaginationResponseDto<TowerResponseDto>> {
        return adminService.getTowers(offsetPaginationRequestDto, sortField)
    }

    @Operation(summary = "미탈퇴 회원 목록 조회 [오프셋]")
    @GetMapping("/members")
    fun getMembers(
        @Valid @ParameterObject offsetPaginationRequestDto: OffsetPaginationRequestDto,
        @RequestParam(defaultValue = "ID") sortField: BaseSortField
    ): SuccessDataResponse<OffsetPaginationResponseDto<MemberResponseDto>> {
        return adminService.getMembers(offsetPaginationRequestDto, sortField)
    }

    @Operation(summary = "회원 권한 변경")
    @PutMapping("/members/{memberId}/roles")
    fun updateMemberRole(
        @PathVariable memberId: Long,
        @RequestBody memberRoleUpdateRequestDto: MemberRoleUpdateRequestDto
    ): SuccessDataResponse<MemberResponseDto> {
        return adminService.updateMemberRole(
            memberId = memberId,
            memberRoleUpdateRequestDto = memberRoleUpdateRequestDto
        )
    }

    @Operation(summary = "회원 상태 변경")
    @PutMapping("/members/{memberId}/status")
    fun updateMemberStatus(
        @PathVariable memberId: Long,
        @Valid @RequestBody memberStatusUpdateRequestDto: MemberStatusUpdateRequestDto
    ): SuccessDataResponse<MemberResponseDto> {
        return adminService.updateMemberStatus(
            memberId = memberId,
            memberStatusUpdateRequestDto = memberStatusUpdateRequestDto
        )
    }

}
