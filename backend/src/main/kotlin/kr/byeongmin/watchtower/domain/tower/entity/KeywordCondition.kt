package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException

/**
 * @property [keyword] 감지 대상 키워드
 * @property [isSatisfiedAtAppearance] true이면 키워드가 새로 등장할 때, false이면 사라질 때 조건을 충족
 */
@Entity
@DiscriminatorValue("KEYWORD")
class KeywordCondition private constructor(
    tower: Tower,
    keyword: String,
    isSatisfiedAtAppearance: Boolean
) : TowerCondition(tower) {
    var keyword: String = keyword
        protected set

    var isSatisfiedAtAppearance: Boolean = isSatisfiedAtAppearance
        protected set

    fun changeKeyword(keyword: String) {
        sanityCheck(keyword)
        this.keyword = keyword
    }

    fun changeSatisfactionDirection(isSatisfiedAtAppearance: Boolean) {
        this.isSatisfiedAtAppearance = isSatisfiedAtAppearance
    }

    override fun isSatisfied(previousHtml: String, currentHtml: String): Boolean {
        val previousOccurrenceCount = countKeywordAppearances(previousHtml)
        val currentOccurrenceCount = countKeywordAppearances(currentHtml)

        return if (isSatisfiedAtAppearance) {
            currentOccurrenceCount > previousOccurrenceCount
        } else {
            currentOccurrenceCount < previousOccurrenceCount
        }
    }

    /** html에서 지정한 키워드가 나타난 횟수 카운트 */
    // TODO: html 길이에 비례해서 반복 횟수가 크게 증가함. 다른 방식으로 개선이 필요해보임
    private fun countKeywordAppearances(html: String): Int {
        var appearanceCount = 0
        var searchStartIndex = 0

        while (true) {
            val appearanceIndex = html.indexOf(keyword, startIndex = searchStartIndex)
            if (appearanceIndex < 0) {
                return appearanceCount
            }

            appearanceCount++
            searchStartIndex = appearanceIndex + 1
        }
    }

    companion object {
        private fun sanityCheck(keyword: String) {
            if (keyword.isBlank()) {
                throw BusinessException(CommonError.INVALID_INPUT_VALUE)
            }
        }

        fun from(
            tower: Tower,
            keyword: String,
            isSatisfiedAtAppearance: Boolean
        ): KeywordCondition {
            sanityCheck(keyword)

            val condition = KeywordCondition(
                tower = tower,
                keyword = keyword,
                isSatisfiedAtAppearance = isSatisfiedAtAppearance
            )
            tower.addCondition(condition)
            return condition
        }
    }
}
