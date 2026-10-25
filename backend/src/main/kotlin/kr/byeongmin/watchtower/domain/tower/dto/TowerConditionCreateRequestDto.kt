package kr.byeongmin.watchtower.domain.tower.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.constraints.NotBlank
import kr.byeongmin.watchtower.domain.tower.entity.HtmlCondition
import kr.byeongmin.watchtower.domain.tower.entity.KeywordCondition
import kr.byeongmin.watchtower.domain.tower.entity.RegexCondition
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerCondition

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RegexConditionCreateRequestDto::class, name = "REGEX"),
    JsonSubTypes.Type(value = KeywordConditionCreateRequestDto::class, name = "KEYWORD"),
    JsonSubTypes.Type(value = HtmlConditionCreateRequestDto::class, name = "HTML")
)
sealed interface TowerConditionCreateRequestDto {
    fun to(tower: Tower): TowerCondition
}

data class RegexConditionCreateRequestDto(
    @field:NotBlank
    val regex: String,
    val isSatisfiedAtAppearance: Boolean
) : TowerConditionCreateRequestDto {
    override fun to(tower: Tower): TowerCondition {
        return RegexCondition.from(
            tower = tower,
            regex = regex,
            isSatisfiedAtAppearance = isSatisfiedAtAppearance
        )
    }
}

data class KeywordConditionCreateRequestDto(
    @field:NotBlank
    val keyword: String,
    val isSatisfiedAtAppearance: Boolean
) : TowerConditionCreateRequestDto {
    override fun to(tower: Tower): TowerCondition {
        return KeywordCondition.from(
            tower = tower,
            keyword = keyword,
            isSatisfiedAtAppearance = isSatisfiedAtAppearance
        )
    }
}

data object HtmlConditionCreateRequestDto : TowerConditionCreateRequestDto {
    override fun to(tower: Tower): TowerCondition {
        return HtmlCondition.from(tower = tower)
    }
}
