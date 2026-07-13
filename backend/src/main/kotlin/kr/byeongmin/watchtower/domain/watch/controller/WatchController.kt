package kr.byeongmin.watchtower.domain.watch.controller

import io.swagger.v3.oas.annotations.Operation
import kr.byeongmin.watchtower.global.response.SuccessResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/watches")
class WatchController {
    @Operation(summary = "와치 등록")
    @PostMapping
    fun createWatch(): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "실시간 인기 URL 조회")
    @GetMapping("/trending")
    fun getTrendingWatches(): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "와치 단건 상세 조회")
    @GetMapping("/{watchId}")
    fun getWatch(
        @PathVariable watchId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "와치 수정(일시정지/재개 포함)")
    @PatchMapping("/{watchId}")
    fun updateWatch(
        @PathVariable watchId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "와치 삭제")
    @DeleteMapping("/{watchId}")
    fun deleteWatch(
        @PathVariable watchId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "테스트 알림 발송")
    @PostMapping("/{watchId}/notify")
    fun sendTestNotification(
        @PathVariable watchId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "와치 조건 목록 조회")
    @GetMapping("/{watchId}/conditions")
    fun getWatchConditions(
        @PathVariable watchId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "와치 조건 추가")
    @PostMapping("/{watchId}/conditions")
    fun createWatchCondition(
        @PathVariable watchId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "와치 조건 수정")
    @PatchMapping("/{watchId}/conditions/{conditionId}")
    fun updateWatchCondition(
        @PathVariable watchId: Long,
        @PathVariable conditionId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "와치 조건 삭제")
    @DeleteMapping("/{watchId}/conditions/{conditionId}")
    fun deleteWatchCondition(
        @PathVariable watchId: Long,
        @PathVariable conditionId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "스냅샷 타임라인 조회")
    @GetMapping("/{watchId}/snapshots")
    fun listWatchSnapshots(
        @PathVariable watchId: Long,
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "스냅샷 단건 상세 조회")
    @GetMapping("/{watchId}/snapshots/{snapshotId}")
    fun getWatchSnapshot(
        @PathVariable watchId: Long,
        @PathVariable snapshotId: Long,
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }
}