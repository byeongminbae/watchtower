package kr.byeongmin.watchtower.domain.tower.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kr.byeongmin.watchtower.domain.tower.entity.HtmlCondition
import kr.byeongmin.watchtower.domain.tower.entity.KeywordCondition
import kr.byeongmin.watchtower.domain.tower.entity.RegexCondition
import kr.byeongmin.watchtower.domain.tower.entity.TowerCondition
import kr.byeongmin.watchtower.domain.tower.error.TowerError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.utils.ifNullThrow

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RegexConditionResponseDto::class, name = "REGEX"),
    JsonSubTypes.Type(value = KeywordConditionResponseDto::class, name = "KEYWORD"),
    JsonSubTypes.Type(value = HtmlConditionResponseDto::class, name = "HTML")
)
sealed interface TowerConditionResponseDto {
    val id: Long
    val type: String

    companion object {
        fun from(towerCondition: TowerCondition): TowerConditionResponseDto {
            return when (towerCondition) {
                is RegexCondition -> RegexConditionResponseDto.from(towerCondition)
                is KeywordCondition -> KeywordConditionResponseDto.from(towerCondition)
                is HtmlCondition -> HtmlConditionResponseDto.from(towerCondition)
                else -> throw BusinessException(TowerError.UNSUPPORTED_CONDITION)
            }
        }
    }
}

data class RegexConditionResponseDto(
    override val id: Long,
    val regex: String,
    val isSatisfiedAtAppearance: Boolean
) : TowerConditionResponseDto {
    override val type: String = "REGEX"

    companion object {
        fun from(condition: RegexCondition): RegexConditionResponseDto {
            return RegexConditionResponseDto(
                id = condition.id.ifNullThrow(),
                regex = condition.regex,
                isSatisfiedAtAppearance = condition.isSatisfiedAtAppearance
            )
        }
    }
}

data class KeywordConditionResponseDto(
    override val id: Long,
    val keyword: String,
    val isSatisfiedAtAppearance: Boolean
) : TowerConditionResponseDto {
    override val type: String = "KEYWORD"

    companion object {
        fun from(condition: KeywordCondition): KeywordConditionResponseDto {
            return KeywordConditionResponseDto(
                id = condition.id.ifNullThrow(),
                keyword = condition.keyword,
                isSatisfiedAtAppearance = condition.isSatisfiedAtAppearance
            )
        }
    }
}

data class HtmlConditionResponseDto(
    override val id: Long
) : TowerConditionResponseDto {
    override val type: String = "HTML"

    companion object {
        fun from(condition: HtmlCondition): HtmlConditionResponseDto {
            return HtmlConditionResponseDto(id = condition.id.ifNullThrow())
        }
    }
}
