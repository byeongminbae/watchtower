package kr.byeongmin.watchtower.domain.notification.controller

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import kr.byeongmin.watchtower.domain.notification.dto.NotificationChannelCreateRequestDto
import kr.byeongmin.watchtower.domain.notification.dto.NotificationChannelResponseDto
import kr.byeongmin.watchtower.domain.notification.dto.NotificationChannelUpdateRequestDto
import kr.byeongmin.watchtower.domain.notification.service.NotificationChannelService
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.CursorPaginationResponseDto
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.security.MemberId
import kr.byeongmin.watchtower.global.security.MemberOnly
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notification/channels")
@MemberOnly
class NotificationChannelController(
    private val channelService: NotificationChannelService
) {
    @Operation(summary = "알림 채널 등록")
    @PostMapping
    fun createChannel(
        @MemberId memberId: Long,
        @Valid @RequestBody notificationChannelCreateRequestDto: NotificationChannelCreateRequestDto
    ): SuccessDataResponse<NotificationChannelResponseDto> {
        return channelService.createChannel(memberId, notificationChannelCreateRequestDto)
    }

    @Operation(summary = "알림 채널 목록 조회 [커서]")
    @GetMapping
    fun getChannels(
        @MemberId memberId: Long,
        @Valid @ParameterObject cursorPaginationRequestDto: CursorPaginationRequestDto,
        @RequestParam(defaultValue = "ID") sortField: BaseSortField
    ): SuccessDataResponse<CursorPaginationResponseDto<NotificationChannelResponseDto>> {
        return channelService.getChannels(memberId, cursorPaginationRequestDto, sortField)
    }

    @Operation(summary = "알림 채널 조회")
    @GetMapping("/{channelId}")
    fun getChannel(
        @MemberId memberId: Long,
        @PathVariable channelId: Long
    ): SuccessDataResponse<NotificationChannelResponseDto> {
        return channelService.getChannel(memberId, channelId)
    }

    @Operation(summary = "알림 채널 수정")
    @PutMapping("/{channelId}")
    fun updateChannel(
        @MemberId memberId: Long,
        @PathVariable channelId: Long,
        @Valid @RequestBody notificationChannelUpdateRequestDto: NotificationChannelUpdateRequestDto
    ): SuccessDataResponse<NotificationChannelResponseDto> {
        return channelService.updateChannel(memberId, channelId, notificationChannelUpdateRequestDto)
    }

    @Operation(summary = "알림 채널 삭제")
    @DeleteMapping("/{channelId}")
    fun deleteChannel(
        @MemberId memberId: Long,
        @PathVariable channelId: Long
    ): SuccessResponse {
        return channelService.deleteChannel(memberId, channelId)
    }
}
