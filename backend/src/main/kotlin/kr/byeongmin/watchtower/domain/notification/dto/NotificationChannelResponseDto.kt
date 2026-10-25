package kr.byeongmin.watchtower.domain.notification.dto

import kr.byeongmin.watchtower.domain.notification.entity.NotificationChannel
import kr.byeongmin.watchtower.domain.notification.enums.NotificationChannelType
import kr.byeongmin.watchtower.global.utils.ifNullThrow

data class NotificationChannelResponseDto(
    val id: Long,
    val name: String,
    val type: NotificationChannelType,
    val webhookUrl: String
) {
    companion object {
        fun from(channel: NotificationChannel): NotificationChannelResponseDto {
            return NotificationChannelResponseDto(
                id = channel.id.ifNullThrow(),
                name = channel.name,
                type = channel.type,
                webhookUrl = channel.webhookUrl
            )
        }
    }
}
