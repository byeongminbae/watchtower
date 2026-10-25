package kr.byeongmin.watchtower.global.pagination

import com.querydsl.core.types.dsl.Expressions
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CursorQueryTest {
    private val id = Expressions.numberPath(Long::class.javaObjectType, "id")
    private val quantity = Expressions.numberPath(Int::class.javaObjectType, "quantity")

    @Test
    fun `커서 변환 람다가 null을 반환하면 입력 오류로 처리함`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            CursorQuery.createFieldCursorCondition(
                quantity,
                id,
                CursorPaginationRequestDto(cursorId = 1L, cursorValue = "커서값")
            ) { null }
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, exception.error)
    }

    @Test
    fun `첫 페이지에서는 커서값을 해석하지 않고 커서 조건 없이 조회함`() {
        /** when */
        val condition = CursorQuery.createFieldCursorCondition(
            quantity,
            id,
            CursorPaginationRequestDto(cursorId = null, cursorValue = "사용하지 않는 값")
        ) { error("첫 페이지에서 커서값을 해석하면 안 됨") }

        /** then */
        assertNull(condition)
    }
}
