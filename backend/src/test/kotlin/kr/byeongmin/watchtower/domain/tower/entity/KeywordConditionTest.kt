package kr.byeongmin.watchtower.domain.tower.entity

import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeywordConditionTest {
    private val tower = mock<Tower>()

    @Test
    fun `키워드가 겹쳐서 등장해도 등장 횟수의 증가를 감지함`() {
        /** given */
        val condition = KeywordCondition.from(tower, "aa", true)

        /** when */
        val result = condition.isSatisfied(previousHtml = "aa", currentHtml = "aaa")

        /** then */
        assertTrue(result)
    }

    @Test
    fun `키워드 조건에서 감지할 단어를 바꿀 수 있음`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = "재고 있음",
            isSatisfiedAtAppearance = true
        )

        /** when */
        condition.changeKeyword("품절")

        /** then */
        assertEquals("품절", condition.keyword)
    }

    @Test
    fun `키워드 등장 조건은 없던 키워드가 나타나면 충족됨`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = "재고 있음",
            isSatisfiedAtAppearance = true
        )

        /** when */
        val result = condition.isSatisfied(
            previousHtml = "<p>품절</p>",
            currentHtml = "<p>재고 있음</p>"
        )

        /** then */
        assertTrue(result)
    }

    @Test
    fun `키워드 등장 조건은 이미 있던 키워드가 더 많아져도 충족됨`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = "재고 있음",
            isSatisfiedAtAppearance = true
        )

        /** when */
        val result = condition.isSatisfied(
            previousHtml = "<p>재고 있음</p>",
            currentHtml = "<p>재고 있음</p><p>재고 있음</p>"
        )

        /** then */
        assertTrue(result)
    }

    @Test
    fun `키워드 사라짐 조건은 기존 키워드가 없어지면 충족됨`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = "재고 있음",
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
    fun `키워드 사라짐 조건은 기존 키워드가 일부만 없어져도 충족됨`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = "재고 있음",
            isSatisfiedAtAppearance = false
        )

        /** when */
        val result = condition.isSatisfied(
            previousHtml = "<p>재고 있음</p><p>재고 있음</p>",
            currentHtml = "<p>재고 있음</p>"
        )

        /** then */
        assertTrue(result)
    }

    @Test
    fun `키워드 등장 조건은 키워드가 사라져도 충족되지 않음`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = "재고 있음",
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
    fun `키워드 사라짐 조건은 키워드가 나타나도 충족되지 않음`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = "재고 있음",
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
    fun `키워드가 등장한 횟수가 같으면 주변 내용이 바뀌어도 조건을 충족하지 않음`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = "재고 있음",
            isSatisfiedAtAppearance = true
        )

        /** when */
        val result = condition.isSatisfied(
            previousHtml = "<p>재고 있음 1개</p>",
            currentHtml = "<p>재고 있음 5개</p>"
        )

        /** then */
        assertFalse(result)
    }

    @Test
    fun `키워드가 이전 본문과 현재 본문에 모두 없으면 조건을 충족하지 않음`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = "재고 있음",
            isSatisfiedAtAppearance = true
        )

        /** when */
        val result = condition.isSatisfied(
            previousHtml = "<p>품절</p>",
            currentHtml = "<p>일시 품절</p>"
        )

        /** then */
        assertFalse(result)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `공백뿐인 키워드로 조건을 만들거나 변경할 수 없음`(value: String) {
        /** given */
        val condition = KeywordCondition.from(tower, "재고", true)

        /** when */
        val creationError = assertFailsWith<BusinessException> {
            KeywordCondition.from(tower, value, true)
        }
        val updateError = assertFailsWith<BusinessException> {
            condition.changeKeyword(value)
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, creationError.error)
        assertEquals(CommonError.INVALID_INPUT_VALUE, updateError.error)
        assertEquals("재고", condition.keyword)
        verify(tower).addCondition(condition)
        verifyNoMoreInteractions(tower)
    }
}
