package kr.byeongmin.watchtower.domain.tower.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.constraints.NotBlank
import kr.byeongmin.watchtower.domain.tower.entity.DetectionCondition
import kr.byeongmin.watchtower.domain.tower.entity.KeywordCondition
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.enums.PresenceChange
import kr.byeongmin.watchtower.domain.tower.vo.Keyword

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = KeywordConditionCreateRequestDto::class, name = "KEYWORD")
)
sealed interface DetectionConditionCreateRequestDto {
    fun to(tower: Tower): DetectionCondition
}

data class KeywordConditionCreateRequestDto(
    @field:NotBlank
    val keyword: String,
    val satisfiedWhen: PresenceChange
) : DetectionConditionCreateRequestDto {
    override fun to(tower: Tower): DetectionCondition {
        return KeywordCondition.from(
            tower = tower,
            keyword = Keyword(keyword),
            satisfiedWhen = satisfiedWhen
        )
    }
}