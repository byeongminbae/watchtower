package kr.byeongmin.watchtower.global.pagination

import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OffsetPaginationResponseDtoTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `조회한 항목을 변환하고 페이지 번호와 크기 및 전체 건수를 유지함`() {
        /** given */
        val page = PageImpl(listOf("item"), PageRequest.of(2, 10), 21L)

        /** when */
        val response = OffsetPaginationResponseDto.from(page) { it.uppercase() }

        /** then */
        assertEquals(listOf("ITEM"), response.data)
        assertEquals(2, response.page)
        assertEquals(10, response.size)
        assertEquals(21L, response.totalElements)
        assertEquals(3, response.totalPages)
    }

    @Test
    fun `조회 결과가 없으면 항목과 전체 페이지 수가 비어 있음`() {
        /** when */
        val response = OffsetPaginationResponseDto.from(
            PageImpl(emptyList<String>(), PageRequest.of(0, 10), 0L)
        ) { it }

        /** then */
        assertTrue(response.data.isEmpty())
        assertEquals(0L, response.totalElements)
        assertEquals(0, response.totalPages)
    }

    @Test
    fun `페이지 목록 응답에는 항목과 현재 페이지 및 전체 페이지 정보가 포함됨`() {
        /** when */
        val json = objectMapper.readTree(
            objectMapper.writeValueAsBytes(
                OffsetPaginationResponseDto(
                    data = listOf("항목"),
                    page = 2,
                    size = 10,
                    totalElements = 21L,
                    totalPages = 3
                )
            )
        )

        /** then */
        assertEquals("항목", json["data"][0].stringValue())
        assertEquals(2, json["page"].intValue())
        assertEquals(10, json["size"].intValue())
        assertEquals(21L, json["totalElements"].longValue())
        assertEquals(3, json["totalPages"].intValue())
    }
}
