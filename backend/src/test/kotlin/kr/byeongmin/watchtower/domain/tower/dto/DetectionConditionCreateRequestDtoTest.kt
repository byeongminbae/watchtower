package kr.byeongmin.watchtower.domain.tower.dto

import kr.byeongmin.watchtower.domain.tower.enums.PresenceChange
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import kotlin.test.assertEquals

class DetectionConditionCreateRequestDtoTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `키워드 조건은 키워드와 감지 방향을 받음`() {
        /** when */
        val dto = objectMapper.readValue<DetectionConditionCreateRequestDto>(
            """{"type":"KEYWORD","keyword":"재입고","satisfiedWhen":"APPEAR"}"""
        )

        /** then */
        assertEquals(KeywordConditionCreateRequestDto(keyword = "재입고", satisfiedWhen = PresenceChange.APPEAR), dto)
    }
}
