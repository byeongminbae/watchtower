package kr.byeongmin.watchtower.domain.tower.entity

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class HtmlConditionTest {
    private val tower = mock<Tower>()

    @Test
    fun `HTML 변경 조건은 이전 본문과 현재 본문이 다르면 충족됨`() {
        /** given */
        val condition = HtmlCondition.from(tower = tower)

        /** when */
        val result = condition.isSatisfied(
            previousHtml = "<p>재고 있음</p>",
            currentHtml = "<p>품절</p>"
        )

        /** then */
        assertTrue(result)
    }

    @Test
    fun `HTML 변경 조건은 이전 본문과 현재 본문이 같으면 충족되지 않음`() {
        /** given */
        val condition = HtmlCondition.from(tower = tower)

        /** when */
        val result = condition.isSatisfied(
            previousHtml = "<p>재고 있음</p>",
            currentHtml = "<p>재고 있음</p>"
        )

        /** then */
        assertFalse(result)
    }
}
