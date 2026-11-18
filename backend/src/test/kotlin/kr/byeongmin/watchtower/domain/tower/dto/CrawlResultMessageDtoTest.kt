package kr.byeongmin.watchtower.domain.tower.dto

import kr.byeongmin.watchtower.domain.tower.enums.CrawlerResultCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrawlResultMessageDtoTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `크롤러가 보낸 페이지 수집 결과를 UTC 시각과 요청한 회원과 받아온 내용과 함께 읽고 모르는 필드는 무시함`() {
        /** given */
        val content = """
            {"towerId":7,"requestedUrl":"https://example.com","jobStartedAt":"2026-11-03T01:00:00.123456",
            "crawlingStartedAt":"2026-11-03T01:00:01.000Z","crawlingEndedAt":"2026-11-03T01:00:05.123Z",
            "triggeredByMemberId":21,"crawlerResultCode":"FETCHED","finalUrl":"https://example.com/moved","pageHttpStatusCode":200,"addedLater":"value",
            "pageContent":{"textHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","dateTimeIgnoredTextHash":"dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd","detectionAreaSelector":".stock","textObjectName":"towers/7/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.txt",
            "imageHash":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"}}
        """.trimIndent()

        /** when */
        val crawlResultMessageDto = objectMapper.readValue<CrawlResultMessageDto>(content)

        /** then */
        assertTrue(crawlResultMessageDto.isCrawlingSucceed())
        assertEquals(LocalDateTime.of(2026, 11, 3, 1, 0, 0, 123_456_000), crawlResultMessageDto.jobStartedAt)
        assertEquals(LocalDateTime.of(2026, 11, 3, 1, 0, 1), crawlResultMessageDto.crawlingStartedAt)
        assertEquals(LocalDateTime.of(2026, 11, 3, 1, 0, 5, 123_000_000), crawlResultMessageDto.crawlingEndedAt)
        assertEquals(21L, crawlResultMessageDto.triggeredByMemberId)
        assertEquals("https://example.com/moved", crawlResultMessageDto.finalUrl)
        assertEquals(
            CrawlResultMessageDto.PageContent(
                textHash = "a".repeat(64),
                dateTimeIgnoredTextHash = "d".repeat(64),
                detectionAreaSelector = ".stock",
                textObjectName = "towers/7/${"a".repeat(64)}.txt",
                imageHash = "b".repeat(64)
            ),
            crawlResultMessageDto.pageContent
        )
    }

    @Test
    fun `크롤러가 보낸 실패 결과는 실패 이유를 읽고 크롤링 실패로 판정하며 받아온 내용이 없음`() {
        /** given */
        val content = """
            {"towerId":7,"requestedUrl":"https://example.com","jobStartedAt":"2026-11-03T01:00:00.123456",
            "crawlingStartedAt":"2026-11-03T01:00:01.000Z","crawlingEndedAt":"2026-11-03T01:00:31.000Z",
            "triggeredByMemberId":null,"crawlerResultCode":"NAVIGATION_TIMEOUT","finalUrl":null,"pageHttpStatusCode":null,"pageContent":null}
        """.trimIndent()

        /** when */
        val crawlResultMessageDto = objectMapper.readValue<CrawlResultMessageDto>(content)

        /** then */
        assertEquals(CrawlerResultCode.NAVIGATION_TIMEOUT, crawlResultMessageDto.crawlerResultCode)
        assertFalse(crawlResultMessageDto.isCrawlingSucceed())
        assertNull(crawlResultMessageDto.pageContent)
    }

    @Test
    fun `백엔드가 모르는 결과 코드는 UNKNOWN 으로 읽고 크롤링 실패로 판정함`() {
        /** given */
        val content = """
            {"towerId":7,"requestedUrl":"https://example.com","jobStartedAt":"2026-11-03T01:00:00.123456",
            "crawlingStartedAt":"2026-11-03T01:00:01.000Z","crawlingEndedAt":"2026-11-03T01:00:31.000Z",
            "triggeredByMemberId":null,"crawlerResultCode":"ADDED_LATER","finalUrl":null,"pageHttpStatusCode":null,"pageContent":null}
        """.trimIndent()

        /** when */
        val crawlResultMessageDto = objectMapper.readValue<CrawlResultMessageDto>(content)

        /** then */
        assertEquals(CrawlerResultCode.UNKNOWN, crawlResultMessageDto.crawlerResultCode)
        assertFalse(crawlResultMessageDto.isCrawlingSucceed())
    }

    @ParameterizedTest
    @CsvSource("404", "503")
    fun `페이지를 받아왔더라도 2xx 가 아닌 응답이면 크롤링 실패로 판정함`(pageHttpStatusCode: Int) {
        /** given */
        val crawlResultMessageDto = CrawlResultMessageDto(
            towerId = 7L,
            requestedUrl = "https://example.com",
            finalUrl = "https://example.com",
            jobStartedAt = LocalDateTime.of(2026, 11, 3, 1, 0),
            crawlingStartedAt = LocalDateTime.of(2026, 11, 3, 1, 0, 1),
            crawlingEndedAt = LocalDateTime.of(2026, 11, 3, 1, 0, 5),
            triggeredByMemberId = null,
            crawlerResultCode = CrawlerResultCode.FETCHED,
            pageHttpStatusCode = pageHttpStatusCode,
            pageContent = null
        )

        /** when */
        val result = crawlResultMessageDto.isCrawlingSucceed()

        /** then */
        assertFalse(result)
    }
}
