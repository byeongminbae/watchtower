package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.byeongmin.watchtower.domain.tower.enums.PresenceChange
import kr.byeongmin.watchtower.domain.tower.vo.DetectionContent
import kr.byeongmin.watchtower.domain.tower.vo.Keyword

/**
 * @property [satisfiedWhen] 키워드가 새로 등장할 때와 사라질 때 중 조건을 충족하는 시점
 */
@Entity
@DiscriminatorValue("KEYWORD")
class KeywordCondition private constructor(
    tower: Tower,
    keyword: Keyword,
    @Enumerated(EnumType.STRING)
    var satisfiedWhen: PresenceChange
) : DetectionCondition(tower) {
    var keyword: String = keyword.value
        protected set

    fun changeKeyword(keyword: Keyword) {
        this.keyword = keyword.value
    }

    override fun isConditionSatisfied(previous: DetectionContent, current: DetectionContent): Boolean {
        val previousOccurrenceCount = countKeywordAppearances(previous.text)
        val currentOccurrenceCount = countKeywordAppearances(current.text)

        return satisfiedWhen.hasOccurred(previousCount = previousOccurrenceCount, currentCount = currentOccurrenceCount)
    }

    private fun countKeywordAppearances(text: String): Int {
        var appearanceCount = 0
        var searchStartIndex = 0

        while (true) {
            val appearanceIndex = text.indexOf(keyword, startIndex = searchStartIndex)
            if (appearanceIndex < 0) {
                return appearanceCount
            }

            appearanceCount++
            searchStartIndex = appearanceIndex + 1
        }
    }

    companion object {
        fun from(
            tower: Tower,
            keyword: Keyword,
            satisfiedWhen: PresenceChange
        ): KeywordCondition {
            val condition = KeywordCondition(
                tower = tower,
                keyword = keyword,
                satisfiedWhen = satisfiedWhen
            )
            tower.addCondition(condition)
            return condition
        }
    }
}
