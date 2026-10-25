package kr.byeongmin.watchtower.domain.notification.accessor

import kr.byeongmin.watchtower.domain.notification.entity.NotificationChannel
import kr.byeongmin.watchtower.domain.notification.repository.NotificationChannelQuerydslRepository
import kr.byeongmin.watchtower.domain.notification.repository.NotificationChannelRepository
import kr.byeongmin.watchtower.domain.tower.repository.TowerNotificationChannelQuerydslRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NotificationChannelAccessor(
    private val notificationChannelRepository: NotificationChannelRepository,
    private val notificationChannelQuerydslRepository: NotificationChannelQuerydslRepository,
    private val towerNotificationChannelQuerydslRepository: TowerNotificationChannelQuerydslRepository
) {
    fun save(channel: NotificationChannel): NotificationChannel {
        return notificationChannelRepository.save(channel)
    }

    fun findNotificationChannels(
        memberId: Long,
        cursorPaginationRequestDto: CursorPaginationRequestDto,
        sortField: BaseSortField
    ): List<NotificationChannel> {
        return notificationChannelQuerydslRepository.findAllByCursor(memberId, cursorPaginationRequestDto, sortField)
    }

    fun getChannel(memberId: Long, channelId: Long): NotificationChannel {
        return notificationChannelRepository.findByIdAndMemberIdAndMemberDeletedAtIsNullAndDeletedAtIsNull(
            channelId,
            memberId
        )
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }

    fun findByIdsAndMemberId(channelIds: Set<Long>, memberId: Long): List<NotificationChannel> {
        if (channelIds.isEmpty()) {
            return emptyList()
        }

        return notificationChannelRepository.findAllByIdInAndMemberIdAndDeletedAtIsNull(channelIds, memberId)
    }

    fun deleteNotificationChannel(memberId: Long, channelId: Long, deletedAt: LocalDateTime) {
        val channel = getChannel(memberId, channelId)
        towerNotificationChannelQuerydslRepository.deleteByNotificationChannelId(channelId, deletedAt)
        channel.delete()
    }

    fun deleteByMemberId(memberId: Long, deletedAt: LocalDateTime) {
        notificationChannelQuerydslRepository.deleteByMemberId(memberId, deletedAt)
    }
}
