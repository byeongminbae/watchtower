package kr.byeongmin.watchtower.external.oci.queue.dto

import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrawlRequestDtoTest {
    private val objectMapper = jacksonObjectMapper()

    private val tower = mock<Tower> {
        on { id } doReturn 7L
        on { url } doReturn "https://example.com"
    }
    private val latestSnapshot = mock<TowerSnapshot> {
        on { htmlSha256 } doReturn "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }

    @Test
    fun `크롤러가 읽는 형식으로 타워와 UTC 밀리초 단위 실행 예정 시각과 요청한 회원과 가장 최근 스냅샷의 HTML 해시를 보냄`() {
        /** when */
        val json = objectMapper.writeValueAsString(
            CrawlRequestDto.from(tower, latestSnapshot, LocalDateTime.of(2026, 11, 3, 1, 0, 0, 123_456_789), 21L)
        )

        /** then */
        assertEquals(
            """{"towerId":7,"url":"https://example.com","scheduledAt":"2026-11-03T01:00:00.123Z","triggeredByMemberId":21,"previousHtmlSha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}""",
            json
        )
    }

    @Test
    fun `스냅샷이 없는 타워는 크롤러가 비교 없이 저장하도록 직전 버전 HTML 해시를 null 로 보냄`() {
        /** when */
        val json = objectMapper.writeValueAsString(
            CrawlRequestDto.from(tower, null, LocalDateTime.of(2026, 11, 3, 1, 0), null)
        )

        /** then */
        assertTrue(json.contains(""""previousHtmlSha256":null"""))
    }

    @Test
    fun `자동 요청이면 요청한 회원 필드를 빼지 않고 null 로 보냄`() {
        /** when */
        val json = objectMapper.writeValueAsString(
            CrawlRequestDto.from(tower, latestSnapshot, LocalDateTime.of(2026, 11, 3, 1, 0), null)
        )

        /** then */
        assertTrue(json.contains(""""triggeredByMemberId":null"""))
    }
}
