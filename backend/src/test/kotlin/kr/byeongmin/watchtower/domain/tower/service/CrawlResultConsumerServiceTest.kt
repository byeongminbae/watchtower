package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerRequestHistoryAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.client.CrawlQueueClient
import kr.byeongmin.watchtower.domain.tower.client.SnapshotStorageClient
import kr.byeongmin.watchtower.domain.tower.dto.QueueResultMessageDto
import kr.byeongmin.watchtower.domain.tower.dto.TowerCreateRequestDto
import kr.byeongmin.watchtower.domain.tower.entity.KeywordCondition
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerRequestHistory
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.domain.tower.enums.CrawlerResultCode
import kr.byeongmin.watchtower.domain.tower.enums.PresenceChange
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.vo.DetectionContent
import kr.byeongmin.watchtower.domain.tower.vo.Keyword
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.global.alert.AdminNotifier
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.*

class CrawlResultConsumerServiceTest {
    private val crawlQueueClient = mock<CrawlQueueClient>()
    private val towerAccessor = mock<TowerAccessor>()
    private val towerSnapshotAccessor = mock<TowerSnapshotAccessor>()
    private val towerRequestHistoryAccessor = mock<TowerRequestHistoryAccessor>()
    private val memberAccessor = mock<MemberAccessor>()
    private val adminNotifier = mock<AdminNotifier>()
    private val snapshotStorageClient = mock<SnapshotStorageClient>()
    private val transactionManager = mock<PlatformTransactionManager> {
        on { getTransaction(any()) } doReturn mock<TransactionStatus>()
    }
    private val objectMapper = jacksonObjectMapper()
    private val jobEndedAt = LocalDateTime.of(2026, 11, 3, 1, 0, 6)
    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn jobEndedAt
    }
    private val crawlResultConsumerService = CrawlResultConsumerService(
        crawlQueueClient = crawlQueueClient,
        towerAccessor = towerAccessor,
        towerSnapshotAccessor = towerSnapshotAccessor,
        towerRequestHistoryAccessor = towerRequestHistoryAccessor,
        memberAccessor = memberAccessor,
        snapshotStorageClient = snapshotStorageClient,
        objectMapper = objectMapper,
        transactionTemplate = TransactionTemplate(transactionManager),
        adminNotifier = adminNotifier,
        timeProvider = timeProvider
    )

    private val owner = mock<Member>()
    private val textHash = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    private val textObjectName = "towers/7/2026-11-03T01:00:01.000Z.txt"
    private val dateTimeIgnoredTextHash = "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"
    private val previousTextHash = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    private val previousTextObjectName = "towers/7/2026-11-02T01:00:01.000Z.txt"
    private val imageHash = "c".repeat(64)
    private val jobStartedAt = LocalDateTime.of(2026, 11, 3, 1, 0, 0, 123_456_000)
    private val crawlingStartedAt = LocalDateTime.of(2026, 11, 3, 1, 0, 1)
    private val towerCreateRequest = TowerCreateRequestDto(
        name = "타워",
        url = TowerUrl.from("https://example.com"),
        fetchInterval = Duration.ofMinutes(10),
        includeInStat = true
    )
    private val tower = spyTower(towerCreateRequest)

    private fun spyTower(towerCreateRequest: TowerCreateRequestDto): Tower {
        return spy(towerCreateRequest.to(owner)) {
            on { id } doReturn 7L
        }
    }

    /** @param [textObjectName] 크롤러가 직전 버전과 텍스트, 이미지, 영역이 모두 같아서 저장을 생략했으면 null */
    private fun fetchedResultMessage(
        receipt: String,
        httpStatusCode: Int = 200,
        textObjectName: String? = this.textObjectName,
        imageHash: String? = null,
        detectionAreaSelector: String? = null,
        triggeredBy: Long? = null
    ): QueueResultMessageDto {
        return QueueResultMessageDto(
            receipt = receipt,
            content = objectMapper.writeValueAsString(
                mapOf(
                    "towerId" to 7,
                    "requestedUrl" to "https://example.com",
                    "jobStartedAt" to "2026-11-03T01:00:00.123456",
                    "crawlingStartedAt" to "2026-11-03T01:00:01.000Z",
                    "crawlingEndedAt" to "2026-11-03T01:00:05.000Z",
                    "triggeredByMemberId" to triggeredBy,
                    "crawlerResultCode" to "FETCHED",
                    "finalUrl" to "https://example.com/moved",
                    "pageHttpStatusCode" to httpStatusCode,
                    "pageContent" to mapOf(
                        "textHash" to textHash,
                        "dateTimeIgnoredTextHash" to dateTimeIgnoredTextHash,
                        "textObjectName" to textObjectName,
                        "detectionAreaSelector" to detectionAreaSelector,
                        "imageHash" to imageHash
                    )
                )
            )
        )
    }

    private fun failedResultMessage(
        receipt: String,
        towerId: Long,
        crawlerResultCode: String = CrawlerResultCode.NAVIGATION_TIMEOUT.name
    ): QueueResultMessageDto {
        return QueueResultMessageDto(
            receipt = receipt,
            content = """
                {"towerId":$towerId,"requestedUrl":"https://example.com","jobStartedAt":"2026-11-03T01:00:00.123456",
                "crawlingStartedAt":"2026-11-03T01:00:01.000Z","crawlingEndedAt":"2026-11-03T01:00:31.000Z",
                "triggeredByMemberId":null,"crawlerResultCode":"$crawlerResultCode","finalUrl":null,"pageHttpStatusCode":null,"pageContent":null}
            """.trimIndent()
        )
    }

    private fun previousSnapshot(
        textHash: String,
        textObjectName: String,
        imageHash: String?,
        dateTimeIgnoredTextHash: String = "e".repeat(64),
        detectionAreaSelector: String? = null
    ): TowerSnapshot {
        return TowerSnapshot.of(
            towerRequestHistory = mock<TowerRequestHistory> { on { tower } doReturn tower },
            textObjectName = textObjectName,
            textHash = textHash,
            dateTimeIgnoredTextHash = dateTimeIgnoredTextHash,
            detectionAreaSelector = detectionAreaSelector,
            imageHash = imageHash,
            isChangeDetected = false
        )
    }

    @Test
    fun `크롤링 중인 타워가 2xx 로 페이지를 받은 결과는 타워에 반영하고 요청 이력과 새 버전을 저장한 뒤 큐에서 지움`() {
        /** given */
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(
            listOf(fetchedResultMessage("receipt", 200, detectionAreaSelector = ".stock", triggeredBy = 21L))
        )
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(memberAccessor.findMember(21L)).thenReturn(owner)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        val savedHistory = argumentCaptor<TowerRequestHistory>()
        val savedSnapshot = argumentCaptor<TowerSnapshot>()
        verify(towerRequestHistoryAccessor).save(savedHistory.capture())
        verify(towerSnapshotAccessor).save(savedSnapshot.capture())
        assertEquals(TowerStatus.READY, tower.status)
        assertEquals(LocalDateTime.of(2026, 11, 3, 1, 0, 5), tower.lastFetchedAt)
        assertEquals(jobStartedAt, savedHistory.firstValue.jobStartedAt)
        assertEquals(jobEndedAt, savedHistory.firstValue.jobEndedAt)
        assertEquals("https://example.com/moved", savedHistory.firstValue.finalUrl)
        assertEquals(crawlingStartedAt, savedHistory.firstValue.crawlingStartedAt)
        assertEquals(LocalDateTime.of(2026, 11, 3, 1, 0, 5), savedHistory.firstValue.crawlingEndedAt)
        assertEquals(200, savedHistory.firstValue.pageHttpStatusCode)
        assertEquals(CrawlerResultCode.FETCHED, savedHistory.firstValue.crawlerResultCode)
        assertEquals(owner, savedHistory.firstValue.triggeredBy)
        assertEquals(textObjectName, savedSnapshot.firstValue.textObjectName)
        assertEquals(textHash, savedSnapshot.firstValue.textHash)
        assertEquals(dateTimeIgnoredTextHash, savedSnapshot.firstValue.dateTimeIgnoredTextHash)
        assertEquals(".stock", savedSnapshot.firstValue.detectionAreaSelector)
        verify(crawlQueueClient).deleteCrawlResults(listOf("receipt"))
    }

    @Test
    fun `직전 버전이 없는 첫 페이지는 비교하지 않고 변화를 감지하지 않은 버전으로 요청 이력과 연결해 저장함`() {
        /** given */
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(listOf(fetchedResultMessage("receipt", 200)))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(towerSnapshotAccessor.findLatestSnapshot(7L)).thenReturn(null)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        val savedHistory = argumentCaptor<TowerRequestHistory>()
        val savedSnapshot = argumentCaptor<TowerSnapshot>()
        verify(towerRequestHistoryAccessor).save(savedHistory.capture())
        verify(towerSnapshotAccessor).save(savedSnapshot.capture())
        assertFalse(savedSnapshot.firstValue.isChangeDetected)
        assertSame(savedHistory.firstValue, savedSnapshot.firstValue.towerRequestHistory)
        verifyNoInteractions(snapshotStorageClient)
        verify(tower, never()).detectsChange(any(), any())
    }

    @Test
    fun `직전 버전과 비교해 변화를 감지하면 변화를 감지한 버전으로 요청 이력과 연결해 저장함`() {
        /** given */
        val previousSnapshot = previousSnapshot(previousTextHash, previousTextObjectName, imageHash = null)
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(listOf(fetchedResultMessage("receipt", 200)))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(towerSnapshotAccessor.findLatestSnapshot(7L)).thenReturn(previousSnapshot)
        doReturn(true).whenever(tower).detectsChange(any(), any())

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        val savedHistory = argumentCaptor<TowerRequestHistory>()
        val savedSnapshot = argumentCaptor<TowerSnapshot>()
        verify(towerRequestHistoryAccessor).save(savedHistory.capture())
        verify(towerSnapshotAccessor).save(savedSnapshot.capture())
        assertTrue(savedSnapshot.firstValue.isChangeDetected)
        assertSame(savedHistory.firstValue, savedSnapshot.firstValue.towerRequestHistory)
        assertEquals(textObjectName, savedSnapshot.firstValue.textObjectName)
    }

    @Test
    fun `2xx 가 아닌 응답으로 받은 페이지는 다음 비교 대상이 되지 않도록 버전으로 저장하지 않음`() {
        /** given */
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(listOf(fetchedResultMessage("receipt", 503)))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        val savedHistory = argumentCaptor<TowerRequestHistory>()
        verify(towerRequestHistoryAccessor).save(savedHistory.capture())
        assertEquals(503, savedHistory.firstValue.pageHttpStatusCode)
        verify(towerSnapshotAccessor, never()).save(any())
        verifyNoInteractions(snapshotStorageClient)
        verify(tower, never()).detectsChange(any(), any())
    }

    @Test
    fun `텍스트와 이미지가 모두 직전 버전과 같으면 새 버전 없이 요청 이력만 남김`() {
        /** given */
        val previousSnapshot = previousSnapshot(textHash, textObjectName, imageHash = null)
        whenever(crawlQueueClient.fetchCrawlResults())
            .thenReturn(listOf(fetchedResultMessage("receipt", textObjectName = null)))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(towerSnapshotAccessor.findLatestSnapshot(7L)).thenReturn(previousSnapshot)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        val savedHistory = argumentCaptor<TowerRequestHistory>()
        verify(towerRequestHistoryAccessor).save(savedHistory.capture())
        assertEquals(TowerStatus.READY, tower.status)
        verify(towerSnapshotAccessor, never()).save(any())
        verifyNoInteractions(snapshotStorageClient)
        verify(tower, never()).detectsChange(any(), any())
        verify(crawlQueueClient).deleteCrawlResults(listOf("receipt"))
    }

    @Test
    fun `날짜와 시간을 무시하는 타워는 원문 해시가 달라도 날짜와 시간을 뺀 해시가 같으면 새 버전 없이 요청 이력만 남기고 크롤러가 올린 텍스트를 지움`() {
        /** given */
        val previousSnapshot = previousSnapshot(
            previousTextHash,
            previousTextObjectName,
            imageHash = null,
            dateTimeIgnoredTextHash = dateTimeIgnoredTextHash
        )
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(listOf(fetchedResultMessage("receipt")))
        val dateTimeIgnoringTower = spyTower(towerCreateRequest.copy(ignoreDateTime = true))
        whenever(towerAccessor.findTower(7L)).thenReturn(dateTimeIgnoringTower)
        whenever(towerSnapshotAccessor.findLatestSnapshot(7L)).thenReturn(previousSnapshot)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        verify(towerSnapshotAccessor, never()).save(any())
        verify(snapshotStorageClient).deleteObject(textObjectName)
        verify(snapshotStorageClient, never()).getText(any())
        verify(crawlQueueClient).deleteCrawlResults(listOf("receipt"))
    }

    @Test
    fun `날짜와 시간을 무시하지 않는 타워는 날짜와 시간을 뺀 해시가 같아도 원문 해시로 비교해 새 버전을 저장함`() {
        /** given */
        val previousSnapshot = previousSnapshot(
            previousTextHash,
            previousTextObjectName,
            imageHash = null,
            dateTimeIgnoredTextHash = dateTimeIgnoredTextHash
        )
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(listOf(fetchedResultMessage("receipt")))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(towerSnapshotAccessor.findLatestSnapshot(7L)).thenReturn(previousSnapshot)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        val previous = argumentCaptor<DetectionContent>()
        val current = argumentCaptor<DetectionContent>()
        verify(tower).detectsChange(previous.capture(), current.capture())
        assertEquals(previousTextHash, previous.firstValue.textHash)
        assertEquals(textHash, current.firstValue.textHash)
        verify(towerSnapshotAccessor).save(any())
    }

    @Test
    fun `감시 영역을 바꾼 뒤 첫 결과는 텍스트가 달라도 비교하지 않고 새 영역의 기준 버전으로 저장함`() {
        /** given */
        val previousSnapshot = previousSnapshot(previousTextHash, previousTextObjectName, imageHash = null, detectionAreaSelector = null)
        whenever(crawlQueueClient.fetchCrawlResults())
            .thenReturn(listOf(fetchedResultMessage("receipt", detectionAreaSelector = ".stock")))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(towerSnapshotAccessor.findLatestSnapshot(7L)).thenReturn(previousSnapshot)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        val savedSnapshot = argumentCaptor<TowerSnapshot>()
        verify(towerSnapshotAccessor).save(savedSnapshot.capture())
        assertFalse(savedSnapshot.firstValue.isChangeDetected)
        assertEquals(".stock", savedSnapshot.firstValue.detectionAreaSelector)
    }

    @Test
    fun `텍스트가 같아도 감시 영역이 바뀌었으면 새 영역의 기준 버전을 저장함`() {
        /** given */
        val previousSnapshot = previousSnapshot(textHash, previousTextObjectName, imageHash = null, detectionAreaSelector = ".price")
        whenever(crawlQueueClient.fetchCrawlResults())
            .thenReturn(listOf(fetchedResultMessage("receipt", detectionAreaSelector = ".stock")))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(towerSnapshotAccessor.findLatestSnapshot(7L)).thenReturn(previousSnapshot)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        val savedSnapshot = argumentCaptor<TowerSnapshot>()
        verify(towerSnapshotAccessor).save(savedSnapshot.capture())
        assertEquals(".stock", savedSnapshot.firstValue.detectionAreaSelector)
    }

    @Test
    fun `텍스트가 필요한 감지 조건이 없으면 버킷에서 텍스트를 읽지 않고 해시로만 판정함`() {
        /** given */
        val previousSnapshot = previousSnapshot(previousTextHash, previousTextObjectName, imageHash = null)
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(listOf(fetchedResultMessage("receipt")))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(towerSnapshotAccessor.findLatestSnapshot(7L)).thenReturn(previousSnapshot)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        val savedSnapshot = argumentCaptor<TowerSnapshot>()
        verify(towerSnapshotAccessor).save(savedSnapshot.capture())
        assertTrue(savedSnapshot.firstValue.isChangeDetected)
        verifyNoInteractions(snapshotStorageClient)
    }

    @Test
    fun `키워드 조건이 있으면 직전 버전과 이번 버전의 텍스트를 버킷에서 읽어 판정함`() {
        /** given */
        val previousSnapshot = previousSnapshot(previousTextHash, previousTextObjectName, imageHash = null)
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(listOf(fetchedResultMessage("receipt")))
        val keywordOnlyTower = spyTower(towerCreateRequest.copy(detectTextChange = false))
        whenever(towerAccessor.findTower(7L)).thenReturn(keywordOnlyTower)
        KeywordCondition.from(keywordOnlyTower, Keyword("재입고"), PresenceChange.APPEAR)
        whenever(towerSnapshotAccessor.findLatestSnapshot(7L)).thenReturn(previousSnapshot)
        whenever(snapshotStorageClient.getText(previousTextObjectName)).thenReturn("품절")
        whenever(snapshotStorageClient.getText(textObjectName)).thenReturn("재입고")

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        val savedSnapshot = argumentCaptor<TowerSnapshot>()
        verify(towerSnapshotAccessor).save(savedSnapshot.capture())
        assertTrue(savedSnapshot.firstValue.isChangeDetected)
        verify(snapshotStorageClient).getText(previousTextObjectName)
        verify(snapshotStorageClient).getText(textObjectName)
    }

    @Test
    fun `텍스트는 같고 이미지만 바뀌면 직전 버전의 텍스트 객체가 아닌 크롤러가 새로 저장한 객체를 가리키는 새 버전을 저장함`() {
        /** given */
        val previousSnapshot = previousSnapshot(textHash, previousTextObjectName, imageHash = "d".repeat(64))
        whenever(crawlQueueClient.fetchCrawlResults())
            .thenReturn(listOf(fetchedResultMessage("receipt", imageHash = imageHash)))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(towerSnapshotAccessor.findLatestSnapshot(7L)).thenReturn(previousSnapshot)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        val savedSnapshot = argumentCaptor<TowerSnapshot>()
        verify(towerSnapshotAccessor).save(savedSnapshot.capture())
        assertEquals(textObjectName, savedSnapshot.firstValue.textObjectName)
        assertEquals(imageHash, savedSnapshot.firstValue.imageHash)
        verify(crawlQueueClient).deleteCrawlResults(listOf("receipt"))
    }

    @Test
    fun `크롤러가 저장을 생략한 결과는 지금의 직전 버전과 달라도 새 버전 없이 요청 이력만 남기고 큐에서 지움`() {
        /** given */
        val previousSnapshot = previousSnapshot(previousTextHash, previousTextObjectName, imageHash = null)
        whenever(crawlQueueClient.fetchCrawlResults())
            .thenReturn(listOf(fetchedResultMessage("receipt", textObjectName = null)))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(towerSnapshotAccessor.findLatestSnapshot(7L)).thenReturn(previousSnapshot)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        verify(towerRequestHistoryAccessor).save(any())
        assertEquals(TowerStatus.READY, tower.status)
        verify(towerSnapshotAccessor, never()).save(any())
        verify(tower, never()).detectsChange(any(), any())
        verify(crawlQueueClient).deleteCrawlResults(listOf("receipt"))
    }

    @Test
    fun `키워드 조건 판정에 필요한 텍스트를 버킷에서 읽지 못하면 반영하지 않고 큐에 남겨 다시 전달받음`() {
        /** given */
        val previousSnapshot = previousSnapshot(previousTextHash, previousTextObjectName, imageHash = null)
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(listOf(fetchedResultMessage("receipt", 200)))
        val keywordOnlyTower = spyTower(towerCreateRequest.copy(detectTextChange = false))
        whenever(towerAccessor.findTower(7L)).thenReturn(keywordOnlyTower)
        KeywordCondition.from(keywordOnlyTower, Keyword("재입고"), PresenceChange.APPEAR)
        whenever(towerSnapshotAccessor.findLatestSnapshot(7L)).thenReturn(previousSnapshot)
        whenever(snapshotStorageClient.getText(any()))
            .thenThrow(BusinessException(CommonError.EXTERNAL_API_ERROR))

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        verify(towerSnapshotAccessor, never()).save(any())
        verify(crawlQueueClient, never()).deleteCrawlResults(any())
    }

    @Test
    fun `크롤링이 실패한 결과는 타워를 실패 상태로 바꾸고 실패 이유를 타워와 요청 이력에 남김`() {
        /** given */
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(listOf(failedResultMessage("receipt", towerId = 7L)))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        val savedHistory = argumentCaptor<TowerRequestHistory>()
        verify(towerRequestHistoryAccessor).save(savedHistory.capture())
        assertEquals(TowerStatus.ERROR, tower.status)
        assertEquals(CrawlerResultCode.NAVIGATION_TIMEOUT, tower.lastCrawlerResultCode)
        assertNull(savedHistory.firstValue.pageHttpStatusCode)
        assertEquals(CrawlerResultCode.NAVIGATION_TIMEOUT, savedHistory.firstValue.crawlerResultCode)
        assertNull(savedHistory.firstValue.triggeredBy)
        verify(towerSnapshotAccessor, never()).save(any())
        verify(crawlQueueClient).deleteCrawlResults(listOf("receipt"))
    }

    @Test
    fun `백엔드가 모르는 결과 코드는 UNKNOWN 실패로 반영해 큐에서 지우고 운영자에게 받은 내용을 알림`() {
        /** given */
        whenever(crawlQueueClient.fetchCrawlResults())
            .thenReturn(listOf(failedResultMessage("receipt", towerId = 7L, crawlerResultCode = "ADDED_LATER")))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        val savedHistory = argumentCaptor<TowerRequestHistory>()
        verify(towerRequestHistoryAccessor).save(savedHistory.capture())
        assertEquals(TowerStatus.ERROR, tower.status)
        assertEquals(CrawlerResultCode.UNKNOWN, savedHistory.firstValue.crawlerResultCode)
        verify(adminNotifier).sendWarningAlert(argThat { contains("ADDED_LATER") })
        verify(crawlQueueClient).deleteCrawlResults(listOf("receipt"))
    }

    @Test
    fun `요청 이력이 이미 있는 결과를 다시 받으면 아무것도 반영하지 않고 큐에서 지움`() {
        /** given */
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(listOf(fetchedResultMessage("receipt", 200)))
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(towerRequestHistoryAccessor.isCrawlResultApplied(7L, crawlingStartedAt)).thenReturn(true)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        assertEquals(TowerStatus.RUNNING, tower.status)
        verify(towerRequestHistoryAccessor, never()).save(any())
        verify(towerSnapshotAccessor, never()).save(any())
        verify(crawlQueueClient).deleteCrawlResults(listOf("receipt"))
    }

    @Test
    fun `삭제됐거나 없는 타워의 결과는 반영하지 않고 큐에서 지움`() {
        /** given */
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(listOf(fetchedResultMessage("receipt", 200)))
        whenever(towerAccessor.findTower(7L)).thenReturn(null)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        verify(towerRequestHistoryAccessor, never()).save(any())
        verify(towerSnapshotAccessor, never()).save(any())
        verify(crawlQueueClient).deleteCrawlResults(listOf("receipt"))
    }

    @Test
    fun `형식이 잘못됐거나 반영하지 못한 결과는 큐에 남겨 다시 전달받고 나머지 결과만 지움`() {
        /** given */
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(
            listOf(
                failedResultMessage("applied", towerId = 7L),
                QueueResultMessageDto(receipt = "malformed", content = "형식이 잘못된 결과"),
                failedResultMessage("apply-failed", towerId = 8L)
            )
        )
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)
        whenever(towerAccessor.findTower(8L)).thenThrow(IllegalStateException("DB 연결 끊김"))

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        verify(crawlQueueClient).deleteCrawlResults(listOf("applied"))
    }

    @Test
    fun `반영하지 못한 결과가 있으면 결과마다 운영자에게 알림을 보냄`() {
        /** given */
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(
            listOf(
                failedResultMessage("applied", towerId = 7L),
                QueueResultMessageDto(receipt = "malformed", content = "형식이 잘못된 결과")
            )
        )
        whenever(towerAccessor.findTower(7L)).thenReturn(tower)

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        verify(adminNotifier).sendErrorAlert(argThat { contains("형식이 잘못된 결과") }, any())
        verifyNoMoreInteractions(adminNotifier)
    }

    @Test
    fun `받은 결과가 없으면 큐에 지우기를 요청하지 않음`() {
        /** given */
        whenever(crawlQueueClient.fetchCrawlResults()).thenReturn(emptyList())

        /** when */
        crawlResultConsumerService.applyCrawlResults()

        /** then */
        verify(crawlQueueClient, never()).deleteCrawlResults(any())
    }
}
