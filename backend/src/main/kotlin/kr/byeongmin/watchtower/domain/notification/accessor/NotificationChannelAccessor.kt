package kr.byeongmin.watchtower.domain.notification.accessor

import kr.byeongmin.watchtower.domain.notification.entity.NotificationChannel
import kr.byeongmin.watchtower.domain.notification.repository.NotificationChannelQuerydslRepository
import kr.byeongmin.watchtower.domain.notification.repository.NotificationChannelRepository
import kr.byeongmin.watchtower.domain.tower.repository.TowerNotificationChannelQuerydslRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import kr.byeongmin.watchtower.global.utils.ifNullThrow
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

    fun findOwnedNotificationChannels(
        memberId: Long,
        cursorPaginationRequestDto: CursorPaginationRequestDto,
        sortField: BaseSortField
    ): List<NotificationChannel> {
        return notificationChannelQuerydslRepository.findAllByCursor(memberId, cursorPaginationRequestDto, sortField)
    }

    fun getOwnedNotificationChannel(memberId: Long, channelId: Long): NotificationChannel {
        return notificationChannelRepository.findByIdAndMemberIdAndMemberDeletedAtIsNullAndDeletedAtIsNull(
            channelId,
            memberId
        )
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }

    fun findOwnedNotificationChannels(channelIds: Set<Long>, memberId: Long): List<NotificationChannel> {
        if (channelIds.isEmpty()) {
            return emptyList()
        }

        return notificationChannelRepository.findAllByIdInAndMemberIdAndDeletedAtIsNull(channelIds, memberId)
    }

    fun deleteNotificationChannel(channel: NotificationChannel, deletedAt: LocalDateTime) {
        towerNotificationChannelQuerydslRepository.deleteByNotificationChannelId(channel.id.ifNullThrow(), deletedAt)
        channel.delete() // 위에는 중간 테이블임
    }

    fun deleteAllOwnedNotificationChannels(memberId: Long, deletedAt: LocalDateTime) {
        notificationChannelQuerydslRepository.deleteByMemberId(memberId, deletedAt)
    }
}
