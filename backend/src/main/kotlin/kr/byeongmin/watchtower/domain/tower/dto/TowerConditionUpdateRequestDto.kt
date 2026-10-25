package kr.byeongmin.watchtower.domain.tower.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.constraints.NotBlank

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RegexConditionUpdateRequestDto::class, name = "REGEX"),
    JsonSubTypes.Type(value = KeywordConditionUpdateRequestDto::class, name = "KEYWORD")
)
sealed interface TowerConditionUpdateRequestDto

data class RegexConditionUpdateRequestDto(
    @field:NotBlank
    val regex: String,
    val isSatisfiedAtAppearance: Boolean
) : TowerConditionUpdateRequestDto

data class KeywordConditionUpdateRequestDto(
    @field:NotBlank
    val keyword: String,
    val isSatisfiedAtAppearance: Boolean
) : TowerConditionUpdateRequestDto
