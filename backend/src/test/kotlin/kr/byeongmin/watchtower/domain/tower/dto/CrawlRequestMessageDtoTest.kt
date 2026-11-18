package kr.byeongmin.watchtower.domain.tower.dto

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrawlRequestMessageDtoTest {
    private val objectMapper = jacksonObjectMapper()

    private val requester = mock<Member> {
        on { id } doReturn 21L
    }
    private val jobStartedAt = LocalDateTime.of(2026, 11, 3, 1, 0, 0, 123_456_000)
    private val latestSnapshot = mock<TowerSnapshot> {
        on { selectTextHash(true) } doReturn "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        on { selectTextHash(false) } doReturn "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
        on { imageHash } doReturn "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        on { hasSameDetectionArea(".stock") } doReturn true
    }

    private fun tower(detectImageChange: Boolean, ignoreDateTime: Boolean, detectionAreaSelector: String? = ".stock"): Tower {
        return mock {
            on { id } doReturn 7L
            on { url } doReturn "https://example.com"
            on { this.detectImageChange } doReturn detectImageChange
            on { this.ignoreDateTime } doReturn ignoreDateTime
            on { this.detectionAreaSelector } doReturn detectionAreaSelector
        }
    }

    @Test
    fun `크롤러가 읽는 형식으로 타워와 추출 대상과 요청한 회원과 가장 최근 스냅샷의 날짜 무시 설정에 맞는 해시들과 작업 시작 시각을 보냄`() {
        /** when */
        val json = objectMapper.writeValueAsString(
            CrawlRequestMessageDto.from(tower(detectImageChange = true, ignoreDateTime = true), latestSnapshot, requester, jobStartedAt)
        )

        /** then */
        assertEquals(
            """{"towerId":7,"url":"https://example.com","extraction":{"detectionAreaSelector":".stock","collectImageHashes":true,"ignoreDateTime":true},"triggeredByMemberId":21,"previousTextHash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","previousImageHash":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb","jobStartedAt":"2026-11-03T01:00:00.123456"}""",
            json
        )
    }

    @Test
    fun `날짜와 시간을 무시하지 않는 타워는 직전 버전의 원문 텍스트 해시를 보냄`() {
        /** when */
        val json = objectMapper.writeValueAsString(
            CrawlRequestMessageDto.from(tower(detectImageChange = false, ignoreDateTime = false), latestSnapshot, null, jobStartedAt)
        )

        /** then */
        assertTrue(json.contains(""""previousTextHash":"cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc""""))
    }

    @Test
    fun `이미지 변화 감지를 끄고 날짜와 시간을 무시하지 않는 타워는 둘 다 false 로 보냄`() {
        /** when */
        val json = objectMapper.writeValueAsString(
            CrawlRequestMessageDto.from(tower(detectImageChange = false, ignoreDateTime = false), latestSnapshot, null, jobStartedAt)
        )

        /** then */
        assertTrue(json.contains(""""collectImageHashes":false,"ignoreDateTime":false"""))
    }

    @Test
    fun `스냅샷이 없는 타워는 크롤러가 비교 없이 저장하도록 직전 버전의 해시를 비워서 보냄`() {
        /** when */
        val json = objectMapper.writeValueAsString(
            CrawlRequestMessageDto.from(tower(detectImageChange = false, ignoreDateTime = false), null, null, jobStartedAt)
        )

        /** then */
        assertTrue(
            json.contains(""""previousTextHash":null,"previousImageHash":null""")
        )
    }

    @Test
    fun `가장 최근 스냅샷의 감시 영역이 지금 설정과 다르면 크롤러가 저장을 생략하지 않도록 직전 버전을 비워서 보냄`() {
        /** when */
        val json = objectMapper.writeValueAsString(
            CrawlRequestMessageDto.from(
                tower(detectImageChange = true, ignoreDateTime = true, detectionAreaSelector = ".price"),
                latestSnapshot,
                null,
                jobStartedAt
            )
        )

        /** then */
        assertTrue(json.contains(""""detectionAreaSelector":".price""""))
        assertTrue(json.contains(""""previousTextHash":null,"previousImageHash":null"""))
    }

    @Test
    fun `자동 요청이면 요청한 회원 필드를 빼지 않고 null 로 보냄`() {
        /** when */
        val json = objectMapper.writeValueAsString(
            CrawlRequestMessageDto.from(tower(detectImageChange = false, ignoreDateTime = false), latestSnapshot, null, jobStartedAt)
        )

        /** then */
        assertTrue(json.contains(""""triggeredByMemberId":null"""))
    }
}
