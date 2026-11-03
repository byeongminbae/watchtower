package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import kr.byeongmin.watchtower.domain.tower.error.TowerError
import kr.byeongmin.watchtower.global.exception.BusinessException

/**
 * HTML 에서 일부분이 달라지면 충족하는 가장 단순한 조건
 */
@Entity
@DiscriminatorValue("HTML")
class HtmlCondition private constructor(
    tower: Tower
) : TowerCondition(tower) {
    override fun isSatisfied(previousHtml: String, currentHtml: String): Boolean {
        return previousHtml != currentHtml
    }

    companion object {
        fun from(tower: Tower): HtmlCondition {
            if (tower.hasHtmlCondition()) {
                throw BusinessException(TowerError.HTML_CONDITION_DUPLICATED)
            }

            val condition = HtmlCondition(tower = tower)
            tower.addCondition(condition)
            return condition
        }
    }
}
