package kr.byeongmin.watchtower.domain.tower.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kr.byeongmin.watchtower.domain.tower.entity.DetectionCondition
import kr.byeongmin.watchtower.domain.tower.entity.KeywordCondition
import kr.byeongmin.watchtower.domain.tower.enums.PresenceChange
import kr.byeongmin.watchtower.domain.tower.error.TowerError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.utils.ifNullThrow

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = KeywordConditionResponseDto::class, name = "KEYWORD")
)
sealed interface DetectionConditionResponseDto {
    val id: Long
    val type: String

    companion object {
        fun from(condition: DetectionCondition): DetectionConditionResponseDto {
            return when (condition) {
                // TODO: OCP 를 위반하는데 다른 방법은 없을까?
                is KeywordCondition -> KeywordConditionResponseDto.from(condition)
                else -> throw BusinessException(TowerError.UNSUPPORTED_CONDITION)
            }
        }
    }
}

data class KeywordConditionResponseDto(
    override val id: Long,
    val keyword: String,
    val satisfiedWhen: PresenceChange
) : DetectionConditionResponseDto {
    override val type: String = "KEYWORD"

    companion object {
        fun from(condition: KeywordCondition): KeywordConditionResponseDto {
            return KeywordConditionResponseDto(
                id = condition.id.ifNullThrow(),
                keyword = condition.keyword,
                satisfiedWhen = condition.satisfiedWhen
            )
        }
    }
}
