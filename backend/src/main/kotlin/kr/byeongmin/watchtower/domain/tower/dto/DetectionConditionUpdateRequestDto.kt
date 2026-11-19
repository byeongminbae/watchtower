package kr.byeongmin.watchtower.domain.tower.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.constraints.NotBlank
import kr.byeongmin.watchtower.domain.tower.enums.PresenceChange

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = KeywordConditionUpdateRequestDto::class, name = "KEYWORD")
)
sealed interface DetectionConditionUpdateRequestDto

data class KeywordConditionUpdateRequestDto(
    @field:NotBlank
    val keyword: String,
    val satisfiedWhen: PresenceChange
) : DetectionConditionUpdateRequestDto
