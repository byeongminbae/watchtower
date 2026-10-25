package kr.byeongmin.watchtower.domain.notification.repository

import kr.byeongmin.watchtower.domain.notification.entity.NotificationChannel
import kr.byeongmin.watchtower.global.repository.BaseRepository

interface NotificationChannelRepository : BaseRepository<NotificationChannel, Long> {
    fun findAllByIdInAndMemberIdAndDeletedAtIsNull(
        channelIds: Set<Long>,
        memberId: Long
    ): List<NotificationChannel>

    fun findByIdAndMemberIdAndMemberDeletedAtIsNullAndDeletedAtIsNull(
        channelId: Long,
        memberId: Long
    ): NotificationChannel?
}
