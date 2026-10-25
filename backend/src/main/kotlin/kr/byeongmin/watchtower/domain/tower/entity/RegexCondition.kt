package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import java.util.regex.PatternSyntaxException

/**
 * 정규식 매칭 갯수가 늘어나거나 줄어들면 충족하는 조건
 * @property [regex] 감지 대상 정규식
 * @property [isSatisfiedAtAppearance] true면 정규식 일치 개수가 증가할 때, false이면 감소할 때 조건을 충족
 */
@Entity
@DiscriminatorValue("REGEX")
class RegexCondition private constructor(
    tower: Tower,
    regex: String,
    isSatisfiedAtAppearance: Boolean
) : TowerCondition(tower) {
    var regex: String = regex
        protected set

    var isSatisfiedAtAppearance: Boolean = isSatisfiedAtAppearance
        protected set

    fun changeRegex(regex: String) {
        sanityCheck(regex)
        this.regex = regex
    }

    fun changeSatisfactionDirection(isSatisfiedAtAppearance: Boolean) {
        this.isSatisfiedAtAppearance = isSatisfiedAtAppearance
    }

    override fun isSatisfied(previousHtml: String, currentHtml: String): Boolean {
        if (previousHtml == currentHtml) {
            return false
        }

        val pattern = compile(regex)
        val previousMatchCount = pattern.findAll(previousHtml).count()
        val currentMatchCount = pattern.findAll(currentHtml).count()

        return if (isSatisfiedAtAppearance) {
            currentMatchCount > previousMatchCount
        } else {
            currentMatchCount < previousMatchCount
        }
    }

    companion object {
        private fun sanityCheck(regex: String) {
            if (regex.isBlank()) {
                throw BusinessException(CommonError.INVALID_INPUT_VALUE)
            }
            compile(regex)
        }

        private fun compile(regex: String): Regex {
            try {
                return Regex(regex)
            } catch (_: PatternSyntaxException) {
                throw BusinessException(CommonError.INVALID_INPUT_VALUE)
            }
        }

        fun from(tower: Tower, regex: String, isSatisfiedAtAppearance: Boolean): RegexCondition {
            sanityCheck(regex)

            val condition = RegexCondition(
                tower = tower,
                regex = regex,
                isSatisfiedAtAppearance = isSatisfiedAtAppearance
            )
            tower.addCondition(condition)
            return condition
        }
    }
}
