package kr.byeongmin.watchtower.domain.tower.entity

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

class RegexConditionTest {
    private val tower = mock<Tower>()

    @Test
    fun `문법이 잘못된 정규식으로 감지 조건을 만들 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            RegexCondition.from(
                tower = tower,
                regex = "[닫히지-않은-대괄호",
                isSatisfiedAtAppearance = true
            )
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, exception.error)
    }

    @Test
    fun `정규식을 잘못된 패턴으로 바꾸려 해도 기존 패턴은 유지됨`() {
        /** given */
        val condition = RegexCondition.from(
            tower = tower,
            regex = "재고\\s*있음",
            isSatisfiedAtAppearance = true
        )

        /** when */
        val exception = assertFailsWith<BusinessException> {
            condition.changeRegex("(열리고-안닫힘")
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, exception.error)
        assertEquals("재고\\s*있음", condition.regex)
    }

    @Test
    fun `정규식 조건에서 감지할 패턴을 바꿀 수 있음`() {
        /** given */
        val condition = RegexCondition.from(
            tower = tower,
            regex = "재고\\s*있음",
            isSatisfiedAtAppearance = true
        )

        /** when */
        condition.changeRegex("품절")

        /** then */
        assertEquals("품절", condition.regex)
    }

    @Test
    fun `정규식 등장 조건은 패턴에 맞는 내용이 새로 나타나면 충족됨`() {
        /** given */
        val condition = RegexCondition.from(
            tower = tower,
            regex = "재고\\s*있음",
            isSatisfiedAtAppearance = true
        )

        /** when */
        val result = condition.isSatisfied(
            previousHtml = "<p>품절</p>",
            currentHtml = "<p>재고    있음</p>"
        )

        /** then */
        assertTrue(result)
    }

    @Test
    fun `정규식 사라짐 조건은 패턴에 맞던 내용이 없어지면 충족됨`() {
        /** given */
        val condition = RegexCondition.from(
            tower = tower,
            regex = "재고\\s*있음",
            isSatisfiedAtAppearance = false
        )

        /** when */
        val result = condition.isSatisfied(
            previousHtml = "<p>재고 있음</p>",
            currentHtml = "<p>품절</p>"
        )

        /** then */
        assertTrue(result)
    }

    @Test
    fun `정규식 등장 조건은 패턴에 맞던 내용이 사라져도 충족되지 않음`() {
        /** given */
        val condition = RegexCondition.from(
            tower = tower,
            regex = "재고\\s*있음",
            isSatisfiedAtAppearance = true
        )

        /** when */
        val result = condition.isSatisfied(
            previousHtml = "<p>재고 있음</p>",
            currentHtml = "<p>품절</p>"
        )

        /** then */
        assertFalse(result)
    }

    @Test
    fun `정규식 사라짐 조건은 패턴에 맞는 내용이 나타나도 충족되지 않음`() {
        /** given */
        val condition = RegexCondition.from(
            tower = tower,
            regex = "재고\\s*있음",
            isSatisfiedAtAppearance = false
        )

        /** when */
        val result = condition.isSatisfied(
            previousHtml = "<p>품절</p>",
            currentHtml = "<p>재고 있음</p>"
        )

        /** then */
        assertFalse(result)
    }

    @Test
    fun `정규식에 일치하는 횟수가 같으면 조건을 충족하지 않음`() {
        /** given */
        val condition = RegexCondition.from(
            tower = tower,
            regex = "재고\\s*있음",
            isSatisfiedAtAppearance = true
        )

        /** when */
        val result = condition.isSatisfied(
            previousHtml = "<p>재고 있음 1개</p>",
            currentHtml = "<p>재고  있음 5개</p>"
        )

        /** then */
        assertFalse(result)
    }

    @Test
    fun `정규식 등장 조건은 패턴에 맞는 내용이 더 많아져도 충족됨`() {
        /** given */
        val condition = RegexCondition.from(
            tower = tower,
            regex = "재고\\s*있음",
            isSatisfiedAtAppearance = true
        )

        /** when */
        val result = condition.isSatisfied(
            previousHtml = "<p>재고 있음</p>",
            currentHtml = "<p>재고 있음</p><p>재고    있음</p>"
        )

        /** then */
        assertTrue(result)
    }

    @Test
    fun `본문이 같으면 정규식에 일치하는 내용이 있어도 조건을 충족하지 않음`() {
        /** given */
        val condition = RegexCondition.from(
            tower = tower,
            regex = "재고\\s*있음",
            isSatisfiedAtAppearance = true
        )
        val html = "<p>재고 있음</p>"

        /** when */
        val result = condition.isSatisfied(
            previousHtml = html,
            currentHtml = html
        )

        /** then */
        assertFalse(result)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `공백뿐인 정규식으로 조건을 만들거나 변경할 수 없음`(value: String) {
        /** given */
        val condition = RegexCondition.from(tower, "재고.*", true)

        /** when */
        val creationError = assertFailsWith<BusinessException> {
            RegexCondition.from(tower, value, true)
        }
        val updateError = assertFailsWith<BusinessException> {
            condition.changeRegex(value)
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, creationError.error)
        assertEquals(CommonError.INVALID_INPUT_VALUE, updateError.error)
        assertEquals("재고.*", condition.regex)
        verify(tower).addCondition(condition)
        verifyNoMoreInteractions(tower)
    }
}
