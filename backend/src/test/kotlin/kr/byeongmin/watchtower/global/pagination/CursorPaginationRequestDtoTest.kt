package kr.byeongmin.watchtower.global.pagination

import kr.byeongmin.watchtower.global.entity.Base
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CursorPaginationRequestDtoTest {
    private class TestItem(override val id: Long?, created: LocalDateTime? = null) : Base() {
        init {
            createdAt = created
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 50])
    fun `다음 페이지 유무를 확인하기 위해 요청한 개수보다 한 건 더 조회함`(size: Int) {
        /** given */
        val request = CursorPaginationRequestDto(size = size)

        /** when, then */
        assertEquals(size.toLong() + 1, request.queryLimit())
    }

    @ParameterizedTest
    @EnumSource(Sort.Direction::class)
    fun `조회 결과의 ID가 순서대로 나열되지 않아도 조회된 순서를 유지하고 마지막 항목을 다음 커서로 사용함`(
        direction: Sort.Direction
    ) {
        /** given */
        val request = CursorPaginationRequestDto(size = 2, sortDirection = direction)
        val transformedIds = mutableListOf<Long?>()

        /** when */
        val response = request.to(listOf(TestItem(40L), TestItem(7L), TestItem(90L)), BaseSortField.ID) {
            transformedIds.add(it.id)
            "항목 ${it.id}"
        }

        /** then */
        assertEquals(listOf("항목 40", "항목 7"), response.data)
        assertEquals(listOf<Long?>(40L, 7L), transformedIds)
        assertEquals(2, response.size)
        assertEquals(7L, response.nextCursorId)
        assertEquals(null, response.nextCursorValue)
        assertEquals(true, response.hasNext)
    }

    @Test
    fun `조회 결과가 요청한 개수와 같으면 다음 페이지가 없음`() {
        /** given */
        val request = CursorPaginationRequestDto(size = 2)

        /** when */
        val response = request.to(listOf(TestItem(5L), TestItem(4L)), BaseSortField.ID) { it.id }

        /** then */
        assertEquals(listOf(5L, 4L), response.data)
        assertEquals(null, response.nextCursorId)
        assertEquals(null, response.nextCursorValue)
        assertEquals(false, response.hasNext)
    }

    @Test
    fun `조회 결과가 요청한 개수보다 적으면 다음 페이지가 없음`() {
        /** when */
        val response = CursorPaginationRequestDto(size = 2).to(listOf(TestItem(5L)), BaseSortField.ID) { it.id }

        /** then */
        assertEquals(listOf(5L), response.data)
        assertEquals(2, response.size)
        assertEquals(null, response.nextCursorId)
        assertEquals(null, response.nextCursorValue)
        assertEquals(false, response.hasNext)
    }

    @Test
    fun `조회 결과가 없으면 빈 목록을 반환하고 다음 페이지가 없음`() {
        /** when */
        val response = CursorPaginationRequestDto(size = 2).to(emptyList<TestItem>(), BaseSortField.ID) { it.id }

        /** then */
        assertTrue(response.data.isEmpty())
        assertEquals(2, response.size)
        assertEquals(null, response.nextCursorId)
        assertEquals(null, response.nextCursorValue)
        assertEquals(false, response.hasNext)
    }

    @Test
    fun `생성 시각으로 이어서 조회할 수 있도록 마지막 반환 항목의 시각을 정밀도 그대로 전달함`() {
        /** given */
        val time = LocalDateTime.parse("2026-10-22T12:34:56.123456")

        /** when */
        val response = CursorPaginationRequestDto(size = 1).to(
            listOf(TestItem(7L, created = time), TestItem(8L, created = time.plusSeconds(1))),
            BaseSortField.CREATED_AT
        ) { it.id }

        /** then */
        assertEquals(7L, response.nextCursorId)
        assertEquals(time.toString(), response.nextCursorValue)
    }

    @Test
    fun `수정 시각이 없는 항목도 ID를 전달해서 다음 페이지를 조회할 수 있음`() {
        /** when */
        val response = CursorPaginationRequestDto(size = 1).to(
            listOf(TestItem(7L), TestItem(8L)), BaseSortField.UPDATED_AT
        ) { it.id }

        /** then */
        assertEquals(7L, response.nextCursorId)
        assertEquals(null, response.nextCursorValue)
        assertTrue(response.hasNext)
    }

    @Test
    fun `정렬값이 없어도 ID가 있으면 다음 페이지 커서로 사용할 수 있음`() {
        /** given */
        val request = CursorPaginationRequestDto(cursorId = 7L, cursorValue = null)

        /** when */
        val result = request.isFirstPage()

        /** then */
        assertEquals(false, result)
    }

    @Test
    fun `정렬 기준이 정한 커서값을 마지막 반환 항목에서 얻음`() {
        /** given */
        val sortField = object : SortField {
            override fun to(direction: Sort.Direction): Sort {
                return Sort.by(direction, "custom")
            }

            override fun getCursorValue(entity: Base): String? {
                return "cursor-${entity.id}"
            }
        }

        /** when */
        val response = CursorPaginationRequestDto(size = 2).to(
            listOf(TestItem(1L), TestItem(2L), TestItem(3L)),
            sortField
        ) { it.id }

        /** then */
        assertEquals("cursor-2", response.nextCursorValue)
    }
}
