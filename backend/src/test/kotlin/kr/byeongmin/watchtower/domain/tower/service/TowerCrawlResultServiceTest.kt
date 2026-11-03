package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerRequestHistoryAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.dto.TowerCreateRequestDto
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerRequestHistory
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.external.oci.queue.client.OciQueueClient
import kr.byeongmin.watchtower.external.oci.queue.dto.QueueResultMessageDto
import kr.byeongmin.watchtower.external.slack.client.SlackClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TowerCrawlResultServiceTest {
    private val ociQueueClient = mock<OciQueueClient>()
    private val towerAccessor = mock<TowerAccessor>()
    private val towerSnapshotAccessor = mock<TowerSnapshotAccessor>()
    private val towerRequestHistoryAccessor = mock<TowerRequestHistoryAccessor>()
    private val memberAccessor = mock<MemberAccessor>()
    private val slackClient = mock<SlackClient>()
    private val transactionManager = mock<PlatformTransactionManager> {
        on { getTransaction(any()) } doReturn mock<TransactionStatus>()
    }
    private val towerCrawlResultService = TowerCrawlResultService(
        ociQueueClient = ociQueueClient,
        towerAccessor = towerAccessor,
        towerSnapshotAccessor = towerSnapshotAccessor,
        towerRequestHistoryAccessor = towerRequestHistoryAccessor,
        memberAccessor = memberAccessor,
        objectMapper = jacksonObjectMapper(),
        transactionTemplate = TransactionTemplate(transactionManager),
        slackClient = slackClient
    )

    private val owner = mock<Member>()
    private val htmlObjectName = "towers/7/2026-11-03T01:00:00.000Z.html"
    private val crawlStartedAt = LocalDateTime.of(2026, 11, 3, 1, 0, 1)
    private val tower = spy(
        Tower.from(
            owner,
            TowerCreateRequestDto(
                name = "타워",
                url = TowerUrl.from("https://example.com"),
                fetchInterval = Duration.ofMinutes(10),
                includeInStat = true
            )
        )
    ) {
        on { id } doReturn 7L
    }

    private fun fetchedResultMessage(receipt: String, httpStatusCode: Int): QueueResultMessageDto {
        return QueueResultMessageDto(
            receipt = receipt,
            content = """
                {"towerId":7,"url":"https://example.com","scheduledAt":"2026-11-03T01:00:00.000Z",
                "crawlStartedAt":"2026-11-03T01:00:01.000Z","crawlEndedAt":"2026-11-03T01:00:05.000Z",
                "triggeredByMemberId":21,"resultStatus":"FETCHED","httpStatusCode":$httpStatusCode,
                "finalUrl":"https://example.com/","htmlObjectName":"$htmlObjectName"}
            """.trimIndent()
        )
    }

    private fun failedResultMessage(receipt: String, towerId: Long): QueueResultMessageDto {
        return QueueResultMessageDto(
            receipt = receipt,
            content = """
                {"towerId":$towerId,"url":"https://example.com","scheduledAt":"2026-11-03T01:00:00.000Z",
                "crawlStartedAt":"2026-11-03T01:00:01.000Z","crawlEndedAt":"2026-11-03T01:00:31.000Z",
                "triggeredByMemberId":null,"resultStatus":"FAILED","errorCode":"CRW_002"}
            """.trimIndent()
        )
    }

    @Test
    fun `크롤링 중인 타워가 페이지를 받은 결과는 타워에 반영하고 요청 이력과 스냅샷을 저장한 뒤 큐에서 지움`() {
        /** given */
        whenever(ociQueueClient.fetchCrawlResults()).thenReturn(listOf(fetchedResultMessage("receipt", 200)))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(memberAccessor.findMember(21L)).thenReturn(owner)

        /** when */
        towerCrawlResultService.applyCrawlResults()

        /** then */
        val savedHistory = argumentCaptor<TowerRequestHistory>()
        val savedSnapshot = argumentCaptor<TowerSnapshot>()
        verify(towerRequestHistoryAccessor).save(savedHistory.capture())
        verify(towerSnapshotAccessor).save(savedSnapshot.capture())
        assertEquals(TowerStatus.CRAWLING_READY, tower.status)
        assertEquals(LocalDateTime.of(2026, 11, 3, 1, 0, 5), tower.lastFetchedAt)
        assertEquals(crawlStartedAt, savedHistory.firstValue.startedAt)
        assertEquals(200, savedHistory.firstValue.httpStatusCode)
        assertNull(savedHistory.firstValue.errorCode)
        assertEquals(owner, savedHistory.firstValue.triggeredBy)
        assertFalse(savedHistory.firstValue.changed)
        assertEquals(htmlObjectName, savedSnapshot.firstValue.htmlContentUrl)
        verify(ociQueueClient).deleteCrawlResults(listOf("receipt"))
    }

    @Test
    fun `크롤링이 실패한 결과는 타워를 실패 상태로 바꾸고 실패 이유를 요청 이력에 남김`() {
        /** given */
        whenever(ociQueueClient.fetchCrawlResults()).thenReturn(listOf(failedResultMessage("receipt", towerId = 7L)))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)

        /** when */
        towerCrawlResultService.applyCrawlResults()

        /** then */
        val savedHistory = argumentCaptor<TowerRequestHistory>()
        verify(towerRequestHistoryAccessor).save(savedHistory.capture())
        assertEquals(TowerStatus.CRAWLING_ERROR, tower.status)
        assertNull(savedHistory.firstValue.httpStatusCode)
        assertEquals("CRW_002", savedHistory.firstValue.errorCode)
        assertNull(savedHistory.firstValue.triggeredBy)
        verify(towerSnapshotAccessor, never()).save(any())
        verify(ociQueueClient).deleteCrawlResults(listOf("receipt"))
    }

    @Test
    fun `요청 이력이 이미 있는 결과를 다시 받으면 아무것도 반영하지 않고 큐에서 지움`() {
        /** given */
        whenever(ociQueueClient.fetchCrawlResults()).thenReturn(listOf(fetchedResultMessage("receipt", 200)))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(towerRequestHistoryAccessor.existsByTowerIdAndStartedAt(7L, crawlStartedAt)).thenReturn(true)

        /** when */
        towerCrawlResultService.applyCrawlResults()

        /** then */
        assertEquals(TowerStatus.CRAWLING, tower.status)
        verify(towerRequestHistoryAccessor, never()).save(any())
        verify(towerSnapshotAccessor, never()).save(any())
        verify(ociQueueClient).deleteCrawlResults(listOf("receipt"))
    }

    @Test
    fun `삭제됐거나 없는 타워의 결과는 반영하지 않고 큐에서 지움`() {
        /** given */
        whenever(ociQueueClient.fetchCrawlResults()).thenReturn(listOf(fetchedResultMessage("receipt", 200)))
        whenever(towerAccessor.findTower(7L)).thenReturn(null)

        /** when */
        towerCrawlResultService.applyCrawlResults()

        /** then */
        verify(towerRequestHistoryAccessor, never()).save(any())
        verify(towerSnapshotAccessor, never()).save(any())
        verify(ociQueueClient).deleteCrawlResults(listOf("receipt"))
    }

    @Test
    fun `형식이 잘못됐거나 반영하지 못한 결과는 큐에 남겨 다시 전달받고 나머지 결과만 지움`() {
        /** given */
        whenever(ociQueueClient.fetchCrawlResults()).thenReturn(
            listOf(
                failedResultMessage("applied", towerId = 7L),
                QueueResultMessageDto(receipt = "malformed", content = "형식이 잘못된 결과"),
                failedResultMessage("apply-failed", towerId = 8L)
            )
        )
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(towerAccessor.findTower(8L)).thenThrow(IllegalStateException("DB 연결 끊김"))

        /** when */
        towerCrawlResultService.applyCrawlResults()

        /** then */
        verify(ociQueueClient).deleteCrawlResults(listOf("applied"))
    }

    @Test
    fun `반영하지 못한 결과가 있으면 결과마다 운영자에게 알림을 보냄`() {
        /** given */
        whenever(ociQueueClient.fetchCrawlResults()).thenReturn(
            listOf(
                failedResultMessage("applied", towerId = 7L),
                QueueResultMessageDto(receipt = "malformed", content = "형식이 잘못된 결과")
            )
        )
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)

        /** when */
        towerCrawlResultService.applyCrawlResults()

        /** then */
        verify(slackClient).sendErrorAlert(argThat { contains("형식이 잘못된 결과") }, any())
        verifyNoMoreInteractions(slackClient)
    }

    @Test
    fun `받은 결과가 없으면 큐에 지우기를 요청하지 않음`() {
        /** given */
        whenever(ociQueueClient.fetchCrawlResults()).thenReturn(emptyList())

        /** when */
        towerCrawlResultService.applyCrawlResults()

        /** then */
        verify(ociQueueClient, never()).deleteCrawlResults(any())
    }
}
