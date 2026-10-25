package kr.byeongmin.watchtower.global.pagination

import kr.byeongmin.watchtower.domain.tower.entity.QTower
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BaseSortFieldTest {
    @ParameterizedTest
    @CsvSource("ID,id", "CREATED_AT,createdAt", "UPDATED_AT,updatedAt")
    fun `정렬 기준을 변환하면 해당 필드명과 요청한 방향을 사용함`(sortField: BaseSortField, fieldName: String) {
        /** when, then */
        assertEquals(Sort.by(Sort.Direction.ASC, fieldName), sortField.to(Sort.Direction.ASC))
        assertEquals(Sort.by(Sort.Direction.DESC, fieldName), sortField.to(Sort.Direction.DESC))
    }

    private class TestItem(created: LocalDateTime, updated: LocalDateTime?) : Base() {
        override val id = 7L

        init {
            createdAt = created
            updatedAt = updated
        }
    }

    @ParameterizedTest
    @CsvSource(
        "ID, NULL",
        "CREATED_AT, 2026-10-22T12:34:56.123456",
        "UPDATED_AT, 2026-10-23T01:23:45.654321",
        nullValues = ["NULL"]
    )
    fun `기본 정렬 기준은 해당 시각을 정밀도 그대로 커서값으로 제공하고 ID 정렬은 별도 값이 없음`(
        sortField: BaseSortField,
        expected: String?
    ) {
        /** given */
        val item = TestItem(
            LocalDateTime.parse("2026-10-22T12:34:56.123456"),
            LocalDateTime.parse("2026-10-23T01:23:45.654321")
        )

        /** when, then */
        assertEquals(expected, sortField.getCursorValue(item))
    }

    @Test
    fun `수정 시각이 없으면 수정 시각 정렬의 커서값도 없음`() {
        /** given */
        val item = TestItem(LocalDateTime.parse("2026-10-22T12:34:56"), null)

        /** when, then */
        assertEquals(null, BaseSortField.UPDATED_AT.getCursorValue(item))
    }

    @ParameterizedTest
    @EnumSource(BaseSortField::class, names = ["CREATED_AT", "UPDATED_AT"])
    fun `시각으로 정렬할 때 날짜 형식이 아닌 커서값은 사용할 수 없음`(sortField: BaseSortField) {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            sortField.getCursorCondition(
                QTower.tower._super,
                QTower.tower.id,
                CursorPaginationRequestDto(cursorId = 7L, cursorValue = "잘못된 날짜")
            )
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, exception.error)
    }
}
