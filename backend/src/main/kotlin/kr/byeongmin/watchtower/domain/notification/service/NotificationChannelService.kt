package kr.byeongmin.watchtower.domain.notification.service

import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.notification.accessor.NotificationChannelAccessor
import kr.byeongmin.watchtower.domain.notification.dto.NotificationChannelCreateRequestDto
import kr.byeongmin.watchtower.domain.notification.dto.NotificationChannelResponseDto
import kr.byeongmin.watchtower.domain.notification.dto.NotificationChannelUpdateRequestDto
import kr.byeongmin.watchtower.domain.notification.entity.NotificationChannel
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.CursorPaginationResponseDto
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationChannelService(
    private val memberAccessor: MemberAccessor,
    private val channelAccessor: NotificationChannelAccessor,
    private val timeProvider: TimeProvider
) {
    @Transactional
    fun createChannel(
        memberId: Long,
        notificationChannelCreateRequestDto: NotificationChannelCreateRequestDto
    ): SuccessDataResponse<NotificationChannelResponseDto> {
        val member = memberAccessor.getMember(memberId)
        val channel = channelAccessor.save(
            NotificationChannel.from(
                member,
                notificationChannelCreateRequestDto.name,
                notificationChannelCreateRequestDto.type,
                notificationChannelCreateRequestDto.webhookUrl
            )
        )
        return SuccessDataResponse(NotificationChannelResponseDto.from(channel))
    }

    @Transactional(readOnly = true)
    fun getChannels(
        memberId: Long,
        cursorPaginationRequestDto: CursorPaginationRequestDto,
        sortField: BaseSortField
    ): SuccessDataResponse<CursorPaginationResponseDto<NotificationChannelResponseDto>> {
        val channels = channelAccessor.findNotificationChannels(memberId, cursorPaginationRequestDto, sortField)
        return SuccessDataResponse(cursorPaginationRequestDto.to(channels, sortField) {
            NotificationChannelResponseDto.from(it)
        })
    }

    @Transactional(readOnly = true)
    fun getChannel(memberId: Long, channelId: Long): SuccessDataResponse<NotificationChannelResponseDto> {
        val channel = channelAccessor.getChannel(memberId, channelId)
        return SuccessDataResponse(NotificationChannelResponseDto.from(channel))
    }

    @Transactional
    fun updateChannel(
        memberId: Long,
        channelId: Long,
        request: NotificationChannelUpdateRequestDto
    ): SuccessDataResponse<NotificationChannelResponseDto> {
        val channel = channelAccessor.getChannel(memberId, channelId)
        channel.update(request.name, request.webhookUrl)
        return SuccessDataResponse(NotificationChannelResponseDto.from(channel))
    }

    @Transactional
    fun deleteChannel(memberId: Long, channelId: Long): SuccessResponse {
        channelAccessor.deleteNotificationChannel(memberId, channelId, timeProvider.now())
        return SuccessResponse()
    }
}
