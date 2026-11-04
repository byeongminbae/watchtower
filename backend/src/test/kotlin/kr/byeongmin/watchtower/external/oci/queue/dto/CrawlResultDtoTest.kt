package kr.byeongmin.watchtower.external.oci.queue.dto

import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CrawlResultDtoTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `크롤러가 보낸 페이지 수집 결과를 UTC 시각과 요청한 회원과 함께 읽음`() {
        /** given */
        val content = """
            {"towerId":7,"url":"https://example.com","scheduledAt":"2026-11-03T01:00:00.000Z",
            "crawlStartedAt":"2026-11-03T01:00:01.000Z","crawlEndedAt":"2026-11-03T01:00:05.123Z","triggeredByMemberId":21,
            "htmlSha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","htmlObjectName":"towers/7/2026-11-03T01:00:00.000Z.html","resultStatus":"FETCHED",
            "httpStatusCode":200,"finalUrl":"https://example.com/"}
        """.trimIndent()

        /** when */
        val crawlResultDto = objectMapper.readValue<CrawlResultDto>(content)

        /** then */
        assertEquals(CrawlResultDto.CrawlResultStatus.FETCHED, crawlResultDto.resultStatus)
        assertEquals(200, crawlResultDto.httpStatusCode)
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", crawlResultDto.htmlSha256)
        assertEquals("towers/7/2026-11-03T01:00:00.000Z.html", crawlResultDto.htmlObjectName)
        assertEquals(LocalDateTime.of(2026, 11, 3, 1, 0, 5, 123_000_000), crawlResultDto.crawlEndedAt)
        assertEquals(21L, crawlResultDto.triggeredByMemberId)
    }

    @Test
    fun `크롤러가 보낸 실패 결과에는 페이지 정보 없이 오류 코드만 있음`() {
        /** given */
        val content = """
            {"towerId":7,"url":"https://example.com","scheduledAt":"2026-11-03T01:00:00.000Z",
            "crawlStartedAt":"2026-11-03T01:00:01.000Z","crawlEndedAt":"2026-11-03T01:00:31.000Z","triggeredByMemberId":null,
            "resultStatus":"FAILED","errorCode":"CRW_002"}
        """.trimIndent()

        /** when */
        val crawlResultDto = objectMapper.readValue<CrawlResultDto>(content)

        /** then */
        assertEquals(CrawlResultDto.CrawlResultStatus.FAILED, crawlResultDto.resultStatus)
        assertEquals("CRW_002", crawlResultDto.errorCode)
        assertNull(crawlResultDto.httpStatusCode)
        assertNull(crawlResultDto.htmlSha256)
        assertNull(crawlResultDto.htmlObjectName)
        assertNull(crawlResultDto.triggeredByMemberId)
    }
}
