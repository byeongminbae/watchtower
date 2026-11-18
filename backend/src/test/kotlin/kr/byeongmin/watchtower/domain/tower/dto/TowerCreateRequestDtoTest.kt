package kr.byeongmin.watchtower.domain.tower.dto

import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TowerCreateRequestDtoTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `감지 옵션과 감시 영역과 조건을 생략하면 페이지 전체의 텍스트 변화만 감지하고 날짜와 시간은 무시하지 않음`() {
        /** when */
        val dto = objectMapper.readValue<TowerCreateRequestDto>(
            """{"name":"재고 감시","url":"https://example.com","fetchInterval":"PT10M","includeInStat":true}"""
        )

        /** then */
        assertTrue(dto.detectTextChange)
        assertFalse(dto.detectImageChange)
        assertFalse(dto.ignoreDateTime)
        assertNull(dto.detectionAreaSelector)
        assertEquals(emptyList(), dto.conditions)
    }
}
