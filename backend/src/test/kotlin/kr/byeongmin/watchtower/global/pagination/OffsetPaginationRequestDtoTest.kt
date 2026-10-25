package kr.byeongmin.watchtower.global.pagination

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import kotlin.test.assertEquals

class OffsetPaginationRequestDtoTest {

    @Test
    fun `조회 조건을 생략하면 검색어 없이 첫 페이지를 20개씩 내림차순으로 조회함`() {
        /** given */
        val request = OffsetPaginationRequestDto()

        /** when */
        val pageable = request.to(BaseSortField.ID)

        /** then */
        assertEquals("", request.query)
        assertEquals(0, pageable.pageNumber)
        assertEquals(20, pageable.pageSize)
        assertEquals(Sort.by(Sort.Direction.DESC, "id"), pageable.sort)
    }

    @Test
    fun `도메인별 정렬 기준을 전달하면 해당 필드를 기준으로 페이지를 조회함`() {
        /** given */
        val request = OffsetPaginationRequestDto(
            page = 1,
            size = 10,
            sortDirection = Sort.Direction.DESC
        )
        val domainSortField = mock<SortField>()
        whenever(domainSortField.to(Sort.Direction.DESC)).thenReturn(Sort.by(Sort.Direction.DESC, "name"))

        /** when */
        val domainPageable = request.to(domainSortField)

        /** then */
        assertEquals(Sort.by(Sort.Direction.DESC, "name"), domainPageable.sort)
        assertEquals(1, domainPageable.pageNumber)
        assertEquals(10, domainPageable.pageSize)
    }
}
