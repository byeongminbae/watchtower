package kr.byeongmin.watchtower.domain.notification.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kr.byeongmin.watchtower.domain.notification.enums.NotificationChannelType
import org.hibernate.validator.constraints.URL

data class NotificationChannelCreateRequestDto(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String?,
    val type: NotificationChannelType,
    @field:NotBlank
    @field:Size(max = 255)
    @field:URL(protocol = "https")
    val webhookUrl: String
)
