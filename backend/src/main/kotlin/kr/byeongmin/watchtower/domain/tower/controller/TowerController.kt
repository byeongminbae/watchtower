package kr.byeongmin.watchtower.domain.tower.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import kr.byeongmin.watchtower.domain.tower.dto.*
import kr.byeongmin.watchtower.domain.tower.service.HotTowerService
import kr.byeongmin.watchtower.domain.tower.service.TowerService
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.security.MemberOnly
import kr.byeongmin.watchtower.global.security.MemberId
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/towers")
class TowerController(
    private val towerService: TowerService,
    private val hotTowerService: HotTowerService
) {
    @Operation(summary = "타워 등록")
    @PostMapping
    @MemberOnly
    fun createTower(
        @MemberId memberId: Long,
        @Valid @RequestBody towerCreateRequestDto: TowerCreateRequestDto
    ): SuccessResponse {
        return towerService.createTower(memberId = memberId, towerCreateRequestDto = towerCreateRequestDto)
    }

    @Operation(summary = "실시간 인기 URL 조회")
    @GetMapping("/trending")
    fun getTrendingTowers(): SuccessDataResponse<List<HotTowerResponseDto>> {
        return hotTowerService.getTrendingTowers()
    }

    @Operation(summary = "타워 단건 상세 조회")
    @GetMapping("/{towerId}")
    @MemberOnly
    fun getTower(
        @MemberId memberId: Long,
        @PathVariable towerId: Long
    ): SuccessDataResponse<TowerResponseDto> {
        return towerService.getTower(memberId = memberId, towerId = towerId)
    }

    @Operation(summary = "타워 수정(일시정지/재개 포함)")
    @PutMapping("/{towerId}")
    @MemberOnly
    fun updateTower(
        @MemberId memberId: Long,
        @PathVariable towerId: Long,
        @Valid @RequestBody towerUpdateRequestDto: TowerUpdateRequestDto
    ): SuccessDataResponse<TowerResponseDto> {
        return towerService.updateTower(
            memberId = memberId,
            towerId = towerId,
            towerUpdateRequestDto = towerUpdateRequestDto
        )
    }

    @Operation(summary = "타워 삭제")
    @DeleteMapping("/{towerId}")
    @MemberOnly
    fun deleteTower(
        @MemberId memberId: Long,
        @PathVariable towerId: Long
    ): SuccessResponse {
        return towerService.deleteTower(memberId = memberId, towerId = towerId)
    }

    /** 현재는 타워 소유권만 확인한다. 서비스의 알림 발송 연동은 미구현이다. */
    @Operation(summary = "테스트 알림 발송")
    @PostMapping("/{towerId}/notify")
    @MemberOnly
    fun sendTestNotification(
        @MemberId memberId: Long,
        @PathVariable towerId: Long
    ): SuccessResponse {
        return towerService.sendTestNotification(memberId = memberId, towerId = towerId)
    }

    @Operation(summary = "타워 조건 목록 조회")
    @GetMapping("/{towerId}/conditions")
    @MemberOnly
    fun getTowerConditions(
        @MemberId memberId: Long,
        @PathVariable towerId: Long
    ): SuccessDataResponse<List<TowerConditionResponseDto>> {
        return towerService.getTowerConditions(memberId = memberId, towerId = towerId)
    }

    @Operation(summary = "기존 타워 조건 추가")
    @PostMapping("/{towerId}/conditions")
    @MemberOnly
    fun createTowerCondition(
        @MemberId memberId: Long,
        @PathVariable towerId: Long,
        @Valid @RequestBody towerConditionCreateRequestDto: TowerConditionCreateRequestDto
    ): SuccessResponse {
        return towerService.createTowerCondition(
            memberId = memberId,
            towerId = towerId,
            towerConditionCreateRequestDto = towerConditionCreateRequestDto
        )
    }

    @Operation(summary = "기존 타워 조건 수정")
    @PutMapping("/{towerId}/conditions/{conditionId}")
    @MemberOnly
    fun updateTowerCondition(
        @MemberId memberId: Long,
        @PathVariable towerId: Long,
        @PathVariable conditionId: Long,
        @Valid @RequestBody towerConditionUpdateRequestDto: TowerConditionUpdateRequestDto
    ): SuccessDataResponse<TowerConditionResponseDto> {
        return towerService.updateTowerCondition(
            memberId = memberId,
            towerId = towerId,
            conditionId = conditionId,
            towerConditionUpdateRequestDto = towerConditionUpdateRequestDto
        )
    }

    @Operation(summary = "기존 타워 조건 삭제")
    @DeleteMapping("/{towerId}/conditions/{conditionId}")
    @MemberOnly
    fun deleteTowerCondition(
        @MemberId memberId: Long,
        @PathVariable towerId: Long,
        @PathVariable conditionId: Long
    ): SuccessResponse {
        return towerService.deleteTowerCondition(
            memberId = memberId,
            towerId = towerId,
            conditionId = conditionId
        )
    }

    @Operation(summary = "스냅샷 조회")
    @GetMapping("/{towerId}/snapshots")
    @MemberOnly
    fun listTowerSnapshots(
        @MemberId memberId: Long,
        @PathVariable towerId: Long
    ): SuccessDataResponse<List<TowerSnapshotResponseDto>> {
        return towerService.getTowerSnapshots(memberId = memberId, towerId = towerId)
    }

}
