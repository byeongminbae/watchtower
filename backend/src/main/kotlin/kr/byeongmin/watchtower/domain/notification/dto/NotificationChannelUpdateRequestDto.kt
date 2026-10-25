package kr.byeongmin.watchtower.domain.notification.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL

data class NotificationChannelUpdateRequestDto(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,
    @field:NotBlank
    @field:Size(max = 255)
    @field:URL(protocol = "https")
    val webhookUrl: String
)
