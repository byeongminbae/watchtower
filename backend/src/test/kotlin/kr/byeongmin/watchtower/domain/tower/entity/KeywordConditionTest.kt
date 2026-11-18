package kr.byeongmin.watchtower.domain.tower.entity

import kr.byeongmin.watchtower.domain.tower.enums.PresenceChange
import kr.byeongmin.watchtower.domain.tower.vo.DetectionContent
import kr.byeongmin.watchtower.domain.tower.vo.Keyword
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeywordConditionTest {
    private val tower = mock<Tower>()

    @Test
    fun `키워드가 겹쳐서 등장해도 등장 횟수의 증가를 감지함`() {
        /** given */
        val condition = KeywordCondition.from(tower, Keyword("aa"), PresenceChange.APPEAR)

        /** when */
        val result = condition.isConditionSatisfied(previous = textContent("aa"), current = textContent("aaa"))

        /** then */
        assertTrue(result)
    }

    @Test
    fun `키워드 조건에서 감지할 단어를 바꿀 수 있음`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = Keyword("재고 있음"),
            satisfiedWhen = PresenceChange.APPEAR
        )

        /** when */
        condition.changeKeyword(Keyword("품절"))

        /** then */
        assertEquals("품절", condition.keyword)
    }

    @Test
    fun `키워드 등장 조건은 없던 키워드가 나타나면 충족됨`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = Keyword("재고 있음"),
            satisfiedWhen = PresenceChange.APPEAR
        )

        /** when */
        val result = condition.isConditionSatisfied(
            previous = textContent("품절"),
            current = textContent("재고 있음")
        )

        /** then */
        assertTrue(result)
    }

    @Test
    fun `키워드 등장 조건은 이미 있던 키워드가 더 많아져도 충족됨`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = Keyword("재고 있음"),
            satisfiedWhen = PresenceChange.APPEAR
        )

        /** when */
        val result = condition.isConditionSatisfied(
            previous = textContent("재고 있음"),
            current = textContent("재고 있음\n재고 있음")
        )

        /** then */
        assertTrue(result)
    }

    @Test
    fun `키워드 사라짐 조건은 기존 키워드가 없어지면 충족됨`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = Keyword("재고 있음"),
            satisfiedWhen = PresenceChange.DISAPPEAR
        )

        /** when */
        val result = condition.isConditionSatisfied(
            previous = textContent("재고 있음"),
            current = textContent("품절")
        )

        /** then */
        assertTrue(result)
    }

    @Test
    fun `키워드 사라짐 조건은 기존 키워드가 일부만 없어져도 충족됨`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = Keyword("재고 있음"),
            satisfiedWhen = PresenceChange.DISAPPEAR
        )

        /** when */
        val result = condition.isConditionSatisfied(
            previous = textContent("재고 있음\n재고 있음"),
            current = textContent("재고 있음")
        )

        /** then */
        assertTrue(result)
    }

    @Test
    fun `키워드 등장 조건은 키워드가 사라져도 충족되지 않음`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = Keyword("재고 있음"),
            satisfiedWhen = PresenceChange.APPEAR
        )

        /** when */
        val result = condition.isConditionSatisfied(
            previous = textContent("재고 있음"),
            current = textContent("품절")
        )

        /** then */
        assertFalse(result)
    }

    @Test
    fun `키워드 사라짐 조건은 키워드가 나타나도 충족되지 않음`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = Keyword("재고 있음"),
            satisfiedWhen = PresenceChange.DISAPPEAR
        )

        /** when */
        val result = condition.isConditionSatisfied(
            previous = textContent("품절"),
            current = textContent("재고 있음")
        )

        /** then */
        assertFalse(result)
    }

    @Test
    fun `키워드가 등장한 횟수가 같으면 주변 내용이 바뀌어도 조건을 충족하지 않음`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = Keyword("재고 있음"),
            satisfiedWhen = PresenceChange.APPEAR
        )

        /** when */
        val result = condition.isConditionSatisfied(
            previous = textContent("재고 있음 1개"),
            current = textContent("재고 있음 5개")
        )

        /** then */
        assertFalse(result)
    }

    @Test
    fun `키워드가 이전 본문과 현재 본문에 모두 없으면 조건을 충족하지 않음`() {
        /** given */
        val condition = KeywordCondition.from(
            tower = tower,
            keyword = Keyword("재고 있음"),
            satisfiedWhen = PresenceChange.APPEAR
        )

        /** when */
        val result = condition.isConditionSatisfied(
            previous = textContent("품절"),
            current = textContent("일시 품절")
        )

        /** then */
        assertFalse(result)
    }

    /** 텍스트만 비교하는 조건용 내용. 해시는 텍스트마다 다른 값이면 충분함 */
    private fun textContent(text: String): DetectionContent {
        return DetectionContent(
            textHash = "hash-of-$text",
            detectionAreaSelector = null,
            imageHash = null,
            textLoader = { text }
        )
    }
}
