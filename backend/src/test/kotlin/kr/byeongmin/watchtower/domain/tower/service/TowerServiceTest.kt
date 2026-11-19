package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.notification.accessor.NotificationChannelAccessor
import kr.byeongmin.watchtower.domain.notification.entity.NotificationChannel
import kr.byeongmin.watchtower.domain.notification.enums.NotificationChannelType
import kr.byeongmin.watchtower.domain.subscription.entity.Plan
import kr.byeongmin.watchtower.domain.tower.accessor.DetectionConditionAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.client.SnapshotStorageClient
import kr.byeongmin.watchtower.domain.tower.dto.*
import kr.byeongmin.watchtower.domain.tower.entity.*
import kr.byeongmin.watchtower.domain.tower.enums.CrawlerResultCode
import kr.byeongmin.watchtower.domain.tower.enums.PresenceChange
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.error.TowerError
import kr.byeongmin.watchtower.domain.tower.policy.TowerPolicy
import kr.byeongmin.watchtower.domain.tower.vo.Keyword
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.external.dns.DnsResolver
import kr.byeongmin.watchtower.external.google.GoogleFaviconClient
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import java.net.InetAddress
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.*

class TowerServiceTest {
    private val towerAccessor = mock<TowerAccessor>()
    private val detectionConditionAccessor = mock<DetectionConditionAccessor>()
    private val towerSnapshotAccessor = mock<TowerSnapshotAccessor>()
    private val memberAccessor = mock<MemberAccessor>()
    private val notificationChannelAccessor = mock<NotificationChannelAccessor>()
    private val googleFaviconClient = mock<GoogleFaviconClient>()
    private val dnsResolver = mock<DnsResolver> {
        on { resolveAll(any()) } doReturn listOf(InetAddress.ofLiteral("8.8.8.8"))
    }
    private val crawlRequestProducerService = mock<CrawlRequestProducerService>()
    private val snapshotStorageClient = mock<SnapshotStorageClient>()
    private val now = LocalDateTime.of(2026, 11, 3, 1, 0)
    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn now
    }

    private val towerService = TowerService(
        towerAccessor = towerAccessor,
        detectionConditionAccessor = detectionConditionAccessor,
        towerSnapshotAccessor = towerSnapshotAccessor,
        memberAccessor = memberAccessor,
        notificationChannelAccessor = notificationChannelAccessor,
        googleFaviconClient = googleFaviconClient,
        dnsResolver = dnsResolver,
        crawlRequestProducerService = crawlRequestProducerService,
        snapshotStorageClient = snapshotStorageClient,
        timeProvider = timeProvider
    )

    private val ownerId = 21L
    private val strangerId = 99L
    private val towerId = 7L
    private val plan = mock<Plan> {
        on { maxConditionQuantity } doReturn 3
    }
    private val owner = mock<Member> {
        on { id } doReturn ownerId
        on { plan } doReturn plan
        on { isTowerAddable(any()) } doReturn true
    }
    private val towerCreateRequest = TowerCreateRequestDto(
        name = "타워",
        url = TowerUrl.from("https://byeongmin.kr/notice"),
        fetchInterval = Duration.ofMinutes(10),
        includeInStat = true,
        notificationChannelIds = emptySet(),
        conditions = emptyList()
    )
    private val towerUpdateRequest = TowerUpdateRequestDto(
        name = "타워",
        fetchInterval = Duration.ofMinutes(10),
        includeInStat = true,
        detectTextChange = true,
        detectImageChange = false,
        ignoreDateTime = false,
        detectionAreaSelector = null
    )
    private val tower = spy(towerCreateRequest.to(owner)) {
        on { id } doReturn towerId
    }

    @BeforeEach
    fun setUp() {
        tower.recordCrawlResult(crawlingEndedAt = now, crawlerResultCode = CrawlerResultCode.FETCHED, isCrawlingSucceed = true)
        whenever(towerAccessor.saveTower(any(), any(), any())).thenReturn(tower)
        whenever(memberAccessor.getMemberForUpdate(ownerId))
            .thenReturn(owner)
        whenever(towerAccessor.getOwnedTower(towerId, ownerId))
            .thenReturn(tower)
        whenever(towerAccessor.getOwnedTower(towerId, strangerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))
        whenever(towerAccessor.getOwnedTowerForUpdate(towerId, ownerId))
            .thenReturn(tower)
        whenever(towerAccessor.getOwnedTowerForUpdate(towerId, strangerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))
        whenever(detectionConditionAccessor.getOwnedDetectionCondition(3L, towerId, strangerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))
    }

    @Test
    fun `타워를 등록할 때 회원의 등록 한도를 확인한 뒤 저장함`() {
        /** given */
        val towerCreateRequestDto = towerCreateRequest.copy(name = "재고-알림")

        /** when */
        val response = towerService.createTower(ownerId, towerCreateRequestDto)

        /** then */
        val savedTower = argumentCaptor<Tower>()
        inOrder(memberAccessor, towerAccessor) {
            verify(memberAccessor).getMemberForUpdate(ownerId)
            verify(towerAccessor).countOwnedTowers(ownerId)
            verify(towerAccessor).saveTower(savedTower.capture(), any(), any())
        }
        assertEquals(towerCreateRequestDto.name, savedTower.firstValue.name)
        assertEquals(towerCreateRequestDto.url.value, savedTower.firstValue.url)
        assertEquals(owner, savedTower.firstValue.createdBy)
        assertEquals(true, response.isSuccess)
    }

    @Test
    fun `타워를 저장한 뒤 저장된 타워의 첫 크롤링을 요청함`() {
        /** when */
        towerService.createTower(ownerId, towerCreateRequest)

        /** then */
        val savedTower = argumentCaptor<Tower>()
        inOrder(towerAccessor, crawlRequestProducerService) {
            verify(towerAccessor).saveTower(savedTower.capture(), any(), any())
            verify(crawlRequestProducerService).requestCrawls(listOf(tower), owner)
        }
    }

    @Test
    fun `첫 크롤링 요청을 큐에 넣지 못하면 타워 등록이 실패함`() {
        /** given */
        whenever(crawlRequestProducerService.requestCrawls(any(), any()))
            .thenThrow(BusinessException(CommonError.EXTERNAL_API_ERROR))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.createTower(ownerId, towerCreateRequest)
        }

        /** then */
        assertEquals(CommonError.EXTERNAL_API_ERROR, exception.error)
    }

    @Test
    fun `크롤링에 실패한 자신의 타워를 재시도하면 크롤링 중으로 바뀌고 크롤링을 요청함`() {
        /** given */
        val failedTower = spy(towerCreateRequest.to(owner)) {
            on { id } doReturn towerId
        }
        failedTower.recordCrawlResult(
            crawlingEndedAt = now,
            crawlerResultCode = CrawlerResultCode.NAVIGATION_TIMEOUT,
            isCrawlingSucceed = false
        )
        whenever(towerAccessor.getOwnedTowerForUpdate(towerId, ownerId)).thenReturn(failedTower)

        /** when */
        val response = towerService.retryCrawl(ownerId, towerId)

        /** then */
        assertEquals(TowerStatus.RUNNING, response.data.status)
        verify(crawlRequestProducerService).requestCrawls(listOf(failedTower), owner)
    }

    @Test
    fun `크롤링에 실패하지 않은 타워는 재시도해도 크롤링을 요청하지 않음`() {
        /** given */
        whenever(towerAccessor.getOwnedTowerForUpdate(towerId, ownerId)).thenReturn(tower)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.retryCrawl(ownerId, towerId)
        }

        /** then */
        assertEquals(TowerError.CRAWL_NOT_RETRYABLE, exception.error)
        verify(crawlRequestProducerService, never()).requestCrawls(any(), anyOrNull())
    }

    @Test
    fun `다른 회원의 타워는 재시도할 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.retryCrawl(strangerId, towerId)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verify(crawlRequestProducerService, never()).requestCrawls(any(), anyOrNull())
    }

    @Test
    fun `자신의 타워를 일시정지하면 일시정지 상태가 되고 크롤링을 요청하지 않음`() {
        /** when */
        val response = towerService.pauseTower(ownerId, towerId)

        /** then */
        assertEquals(TowerStatus.PAUSED, response.data.status)
        verify(crawlRequestProducerService, never()).requestCrawls(any(), anyOrNull())
    }

    @Test
    fun `일시정지한 자신의 타워를 다시 시작하면 크롤링 대기로 바뀌고 크롤링은 바로 요청하지 않음`() {
        /** given */
        tower.pause()

        /** when */
        val response = towerService.resumeTower(ownerId, towerId)

        /** then */
        assertEquals(TowerStatus.READY, response.data.status)
        verifyNoInteractions(crawlRequestProducerService)
    }

    @Test
    fun `일시정지하지 않은 타워는 다시 시작해도 크롤링을 요청하지 않음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.resumeTower(ownerId, towerId)
        }

        /** then */
        assertEquals(TowerError.NOT_RESUMABLE, exception.error)
        verify(crawlRequestProducerService, never()).requestCrawls(any(), anyOrNull())
    }

    @Test
    fun `다른 회원의 타워는 일시정지하거나 다시 시작할 수 없음`() {
        /** given */
        val operations: List<() -> Unit> = listOf(
            { towerService.pauseTower(strangerId, towerId) },
            { towerService.resumeTower(strangerId, towerId) }
        )

        /** when */
        val exceptions = operations.map { operation ->
            assertFailsWith<BusinessException>(block = operation)
        }

        /** then */
        exceptions.forEach { exception ->
            assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        }
    }

    @Test
    fun `큐 기능이 꺼진 환경에서는 첫 크롤링을 요청할 수 없어서 타워 등록이 실패함`() {
        /** given */
        val queueDisabledTowerService = TowerService(
            towerAccessor = towerAccessor,
            detectionConditionAccessor = detectionConditionAccessor,
            towerSnapshotAccessor = towerSnapshotAccessor,
            memberAccessor = memberAccessor,
            notificationChannelAccessor = notificationChannelAccessor,
            googleFaviconClient = googleFaviconClient,
            dnsResolver = dnsResolver,
            crawlRequestProducerService = null,
            snapshotStorageClient = snapshotStorageClient,
            timeProvider = timeProvider
        )

        /** when */
        val exception = assertFailsWith<BusinessException> {
            queueDisabledTowerService.createTower(ownerId, towerCreateRequest)
        }

        /** then */
        assertEquals(CommonError.EXTERNAL_API_ERROR, exception.error)
    }

    @Test
    fun `타워를 등록할 때 선택한 알림 채널을 연결할 수 있음`() {
        /** given */
        val channelId = 31L
        val channel = NotificationChannel.from(
            member = owner,
            name = "개인 디스코드",
            type = NotificationChannelType.DISCORD,
            webhookUrl = "https://discord.com/api/webhooks/example"
        )
        whenever(notificationChannelAccessor.findOwnedNotificationChannels(setOf(channelId), ownerId))
            .thenReturn(listOf(channel))

        /** when */
        towerService.createTower(ownerId, towerCreateRequest.copy(notificationChannelIds = setOf(channelId)))

        /** then */
        val savedTower = argumentCaptor<Tower>()
        val channels = argumentCaptor<List<TowerNotificationChannel>>()
        verify(towerAccessor).saveTower(savedTower.capture(), any(), channels.capture())
        assertEquals(channel, savedTower.firstValue.towerNotificationChannels.single().notificationChannel)
        assertEquals(savedTower.firstValue, channel.towerNotificationChannels.single().tower)
        assertEquals(savedTower.firstValue.towerNotificationChannels, channels.firstValue)
    }

    @Test
    fun `다른 회원의 알림 채널을 연결해 타워를 등록할 수 없음`() {
        /** given */
        val request = towerCreateRequest.copy(notificationChannelIds = setOf(31L))
        whenever(notificationChannelAccessor.findOwnedNotificationChannels(request.notificationChannelIds, ownerId))
            .thenReturn(emptyList())

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.createTower(ownerId, request)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verify(towerAccessor, never()).saveTower(any(), any(), any())
    }

    @Test
    fun `회원이 이미 등록한 타워 수를 기준으로 추가 등록 가능 여부를 판단함`() {
        /** given */
        whenever(towerAccessor.countOwnedTowers(ownerId))
            .thenReturn(19L)

        /** when */
        towerService.createTower(ownerId, towerCreateRequest)

        /** then */
        verify(owner).isTowerAddable(19L)
        verify(towerAccessor).saveTower(any(), any(), any())
    }

    @Test
    fun `이용 한도에 도달한 회원은 타워를 더 등록할 수 없음`() {
        /** given */
        val towerCount = 20L
        whenever(towerAccessor.countOwnedTowers(ownerId))
            .thenReturn(towerCount)
        whenever(owner.isTowerAddable(towerCount))
            .thenReturn(false)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.createTower(ownerId, towerCreateRequest)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_QUOTA_EXCEEDED, exception.error)
        verify(towerAccessor, never()).saveTower(any(), any(), any())
        verifyNoInteractions(detectionConditionAccessor)
    }

    @Test
    fun `타워를 등록하면 사이트의 파비콘도 함께 저장됨`() {
        /** given */
        whenever(googleFaviconClient.buildFaviconUrl(towerCreateRequest.url.value))
            .thenReturn("https://www.google.com/s2/favicons?domain=byeongmin.kr&sz=128")

        /** when */
        towerService.createTower(ownerId, towerCreateRequest)

        /** then */
        val savedTower = argumentCaptor<Tower>()
        verify(towerAccessor).saveTower(savedTower.capture(), any(), any())
        assertEquals("https://www.google.com/s2/favicons?domain=byeongmin.kr&sz=128", savedTower.firstValue.faviconUrl)
    }

    @Test
    fun `사이트의 파비콘을 구하지 못해도 타워를 등록할 수 있음`() {
        /** given */
        whenever(googleFaviconClient.buildFaviconUrl(towerCreateRequest.url.value))
            .thenReturn(null)

        /** when */
        towerService.createTower(ownerId, towerCreateRequest)

        /** then */
        val savedTower = argumentCaptor<Tower>()
        verify(towerAccessor).saveTower(savedTower.capture(), any(), any())
        assertNull(savedTower.firstValue.faviconUrl)
    }

    @Test
    fun `존재하지 않는 회원에게 타워를 등록할 수 없음`() {
        /** given */
        whenever(memberAccessor.getMemberForUpdate(strangerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.createTower(strangerId, towerCreateRequest)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verify(towerAccessor, never()).saveTower(any(), any(), any())
    }

    @Test
    fun `회원은 자신이 소유한 타워의 상세 정보를 조회할 수 있음`() {
        /** when */
        val response = towerService.getTower(ownerId, towerId)

        /** then */
        assertEquals(towerId, response.data.id)
        assertEquals(tower.url, response.data.url)
        assertEquals(TowerStatus.READY, response.data.status)
    }

    @Test
    fun `회원은 자신의 타워 상세 정보에서 마지막 변경의 AI 요약을 확인할 수 있음`() {
        /** given */
        whenever(towerSnapshotAccessor.findLatestChangeAiSummary(towerId)).thenReturn("재입고됨")

        /** when */
        val response = towerService.getTower(ownerId, towerId)

        /** then */
        assertEquals("재입고됨", response.data.latestAiSummary)
    }

    @Test
    fun `존재하지 않는 타워의 상세 정보는 조회할 수 없음`() {
        /** given */
        whenever(towerAccessor.getOwnedTower(towerId, ownerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.getTower(ownerId, towerId)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `회원은 자신의 타워 이름과 수집 주기 및 인기 집계 참여 여부를 변경할 수 있음`() {
        /** when */
        val response = towerService.updateTower(
            ownerId, towerId, towerUpdateRequest.copy(
                name = "변경된-타워-이름",
                fetchInterval = Duration.ofSeconds(1800),
                includeInStat = false
            )
        )

        /** then */
        assertEquals("변경된-타워-이름", response.data.name)
        assertEquals("https://byeongmin.kr/notice", response.data.url)
        assertEquals(Duration.ofSeconds(1800), response.data.fetchInterval)
        assertEquals(false, response.data.includeInStat)
    }

    @Test
    fun `회원은 타워의 텍스트 변화와 이미지 변화 감지 및 날짜와 시간 무시 여부를 변경할 수 있음`() {
        /** when */
        val response = towerService.updateTower(
            ownerId, towerId, towerUpdateRequest.copy(
                detectTextChange = false,
                detectImageChange = true,
                ignoreDateTime = true
            )
        )

        /** then */
        assertFalse(response.data.detectTextChange)
        assertTrue(response.data.detectImageChange)
        assertTrue(response.data.ignoreDateTime)
    }

    @Test
    fun `감지 옵션을 모두 끄고 감지 조건도 없는 타워로는 수정할 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.updateTower(
                ownerId,
                towerId,
                towerUpdateRequest.copy(detectTextChange = false, detectImageChange = false)
            )
        }

        /** then */
        assertEquals(TowerError.HAS_NO_DETECTION, exception.error)
    }

    @Test
    fun `감지 옵션을 모두 끄고 감지 조건도 없는 타워는 등록할 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.createTower(
                ownerId,
                towerCreateRequest.copy(detectTextChange = false, detectImageChange = false, conditions = emptyList())
            )
        }

        /** then */
        assertEquals(TowerError.HAS_NO_DETECTION, exception.error)
        verify(towerAccessor, never()).saveTower(any(), any(), any())
        verifyNoInteractions(crawlRequestProducerService)
    }

    @Test
    fun `회원은 다른 회원의 타워를 수정할 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.updateTower(strangerId, towerId, towerUpdateRequest.copy(name = "남의-타워-이름"))
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `회원이 수정할 수 없는 상태의 타워면 아무것도 변경하지 않음`() {
        /** given */
        val rejection = BusinessException(CommonError.PERMISSION_DENIED)
        doThrow(rejection).whenever(tower).throwIfNotMemberChangeable()

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.updateTower(
                ownerId,
                towerId,
                towerUpdateRequest.copy(name = "변경", fetchInterval = Duration.ofMinutes(30), includeInStat = false)
            )
        }

        /** then */
        assertSame(rejection, exception)
        assertEquals(towerCreateRequest.name, tower.name)
        assertEquals(towerCreateRequest.fetchInterval, tower.fetchInterval)
        assertEquals(towerCreateRequest.includeInStat, tower.includeInStat)
    }

    @Test
    fun `타워 삭제를 요청하면 소유한 타워를 찾아 삭제 처리함`() {
        /** when */
        towerService.deleteTower(ownerId, towerId)

        /** then */
        inOrder(towerAccessor, tower) {
            verify(towerAccessor).getOwnedTower(towerId, ownerId)
            verify(tower).delete()
        }
    }

    @Test
    fun `회원은 다른 회원의 타워를 삭제할 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.deleteTower(strangerId, towerId)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        assertNull(tower.deletedAt)
    }

    @Test
    fun `회원은 다른 회원의 타워로 테스트 알림을 보낼 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.sendTestNotification(strangerId, towerId)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `회원은 자신의 타워에 키워드 감지 조건을 추가할 수 있음`() {
        /** given */
        repeat(2) { KeywordCondition.from(tower, Keyword("기존$it"), PresenceChange.APPEAR) }

        /** when */
        towerService.createDetectionCondition(
            ownerId, towerId, KeywordConditionCreateRequestDto(
                keyword = "재고 있음",
                satisfiedWhen = PresenceChange.APPEAR
            )
        )

        /** then */
        val savedCondition = argumentCaptor<KeywordCondition>()
        inOrder(towerAccessor, detectionConditionAccessor) {
            verify(towerAccessor).getOwnedTowerForUpdate(towerId, ownerId)
            verify(detectionConditionAccessor).save(savedCondition.capture())
        }
        assertEquals("재고 있음", savedCondition.firstValue.keyword)
        assertEquals(PresenceChange.APPEAR, savedCondition.firstValue.satisfiedWhen)
    }

    @ParameterizedTest
    @ValueSource(ints = [3, 4])
    fun `감지 조건이 플랜 한도에 도달하면 조건을 더 추가할 수 없음(경계값 테스트)`(existingConditionCount: Int) {
        /** given */
        repeat(existingConditionCount) { KeywordCondition.from(tower, Keyword("기존$it"), PresenceChange.APPEAR) }

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.createDetectionCondition(
                ownerId, towerId, KeywordConditionCreateRequestDto(
                    keyword = "재고 있음",
                    satisfiedWhen = PresenceChange.DISAPPEAR
                )
            )
        }

        /** then */
        assertEquals(TowerError.CONDITION_QUOTA_EXCEEDED, exception.error)
        verify(detectionConditionAccessor, never()).save(any<KeywordCondition>())
    }

    @Test
    fun `감지 조건 생성에 실패하면 조건을 저장하지 않음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.createDetectionCondition(
                ownerId,
                towerId,
                KeywordConditionCreateRequestDto(keyword = " ", satisfiedWhen = PresenceChange.APPEAR)
            )
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, exception.error)
        verify(detectionConditionAccessor, never()).save(any<DetectionCondition>())
    }

    @Test
    fun `감지 조건을 추가해도 크롤링은 바로 요청하지 않음`() {
        /** when */
        towerService.createDetectionCondition(
            ownerId,
            towerId,
            KeywordConditionCreateRequestDto(keyword = "재입고", satisfiedWhen = PresenceChange.APPEAR)
        )

        /** then */
        verify(detectionConditionAccessor).save(any<KeywordCondition>())
        assertEquals(TowerStatus.READY, tower.status)
        verifyNoInteractions(crawlRequestProducerService)
    }

    @Test
    fun `등록한 감지 조건의 종류와 감지할 내용을 확인할 수 있음`() {
        /** given */
        val keywordCondition = mock<KeywordCondition> {
            on { id } doReturn 1L
            on { keyword } doReturn "재고 있음"
            on { satisfiedWhen } doReturn PresenceChange.APPEAR
        }
        whenever(detectionConditionAccessor.getOwnedDetectionConditions(towerId, ownerId))
            .thenReturn(listOf(keywordCondition))

        /** when */
        val response = towerService.getDetectionConditions(ownerId, towerId)

        /** then */
        assertEquals(
            listOf(KeywordConditionResponseDto(id = 1L, keyword = "재고 있음", satisfiedWhen = PresenceChange.APPEAR)),
            response.data
        )
    }

    @Test
    fun `소유한 타워에 감지 조건이 없으면 조건 목록은 비어 있음`() {
        /** given */
        whenever(detectionConditionAccessor.getOwnedDetectionConditions(towerId, ownerId))
            .thenReturn(emptyList())

        /** when */
        val response = towerService.getDetectionConditions(ownerId, towerId)

        /** then */
        assertTrue(response.data.isEmpty())
    }

    @Test
    fun `회원은 다른 회원의 타워에 있는 감지 조건을 조회할 수 없음`() {
        /** given */
        whenever(detectionConditionAccessor.getOwnedDetectionConditions(towerId, strangerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.getDetectionConditions(strangerId, towerId)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `회원은 타워의 감시 영역을 바꿀 수 있고 크롤링은 바로 요청하지 않음`() {
        /** when */
        val response = towerService.updateTower(ownerId, towerId, towerUpdateRequest.copy(detectionAreaSelector = ".stock"))

        /** then */
        assertEquals(".stock", response.data.detectionAreaSelector)
        assertEquals(TowerStatus.READY, tower.status)
        verifyNoInteractions(crawlRequestProducerService)
    }

    @Test
    fun `감지 조건을 바꿔도 크롤링은 바로 요청하지 않음`() {
        /** given */
        val condition = spy(KeywordCondition.from(tower, Keyword("재고 있음"), PresenceChange.APPEAR)) {
            on { id } doReturn 3L
        }
        whenever(detectionConditionAccessor.getOwnedDetectionCondition(3L, towerId, ownerId))
            .thenReturn(condition)

        /** when */
        towerService.updateDetectionCondition(
            ownerId,
            towerId,
            3L,
            KeywordConditionUpdateRequestDto(keyword = "품절", satisfiedWhen = PresenceChange.DISAPPEAR)
        )

        /** then */
        assertEquals(TowerStatus.READY, tower.status)
        verifyNoInteractions(crawlRequestProducerService)
    }

    @Test
    fun `타워에서 찾을 수 없는 감지 조건은 삭제할 수 없음`() {
        /** given */
        val conditionId = 404L
        whenever(detectionConditionAccessor.getOwnedDetectionCondition(conditionId, towerId, ownerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.deleteDetectionCondition(ownerId, towerId, conditionId)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `감지 조건 삭제를 요청하면 소유한 조건을 찾아 삭제 처리함`() {
        /** given */
        val conditionId = 3L
        val condition = mock<KeywordCondition>()
        whenever(detectionConditionAccessor.getOwnedDetectionCondition(conditionId, towerId, ownerId))
            .thenReturn(condition)

        /** when */
        towerService.deleteDetectionCondition(ownerId, towerId, conditionId)

        /** then */
        verify(condition).delete()
    }

    @Test
    fun `감지 옵션을 모두 끈 타워의 마지막 감지 조건은 삭제할 수 없음`() {
        /** given */
        tower.detectTextChange = false
        val condition = KeywordCondition.from(tower, Keyword("재입고"), PresenceChange.APPEAR)
        whenever(detectionConditionAccessor.getOwnedDetectionCondition(3L, towerId, ownerId))
            .thenReturn(condition)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.deleteDetectionCondition(ownerId, towerId, 3L)
        }

        /** then */
        assertEquals(TowerError.HAS_NO_DETECTION, exception.error)
    }

    @Test
    fun `소유한 타워를 찾을 수 없으면 변경이 감지된 스냅샷을 조회할 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.getTowerChangedSnapshots(strangerId, towerId)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verifyNoInteractions(towerSnapshotAccessor)
    }

    @Test
    fun `회원은 자신의 타워에서 변경이 감지된 스냅샷을 조회할 수 있음`() {
        /** given */
        val snapshotId = 11L
        val snapshot = mock<TowerSnapshot> {
            on { id } doReturn snapshotId
            on { createdAt } doReturn now
        }
        whenever(towerSnapshotAccessor.findChangedSnapshots(tower))
            .thenReturn(listOf(snapshot))

        /** when */
        val response = towerService.getTowerChangedSnapshots(ownerId, towerId)

        /** then */
        assertEquals(1, response.data.size)
        assertEquals(snapshotId, response.data.first().id)
        assertEquals(now, response.data.first().detectedAt)
    }

    @Test
    fun `하이라이트 스크린샷이 있는 스냅샷은 정해진 시간 동안만 읽을 수 있는 URL 을 함께 내려줌`() {
        /** given */
        val objectName = "towers/7/2026-11-04T01:00:00.000Z.png"
        val readUrl = "https://objectstorage.ap-osaka-1.oraclecloud.com/p/token/n/ns/b/bucket/o/$objectName"
        val snapshot = mock<TowerSnapshot> {
            on { id } doReturn 11L
            on { createdAt } doReturn now
            on { highlightedScreenshotObjectName } doReturn objectName
        }
        whenever(towerSnapshotAccessor.findChangedSnapshots(tower)).thenReturn(listOf(snapshot))
        whenever(
            snapshotStorageClient.createReadUrl(
                objectName,
                timeProvider.now().plus(TowerPolicy.SNAPSHOT_READ_URL_VALIDITY)
            )
        ).thenReturn(readUrl)

        /** when */
        val response = towerService.getTowerChangedSnapshots(ownerId, towerId)

        /** then */
        assertEquals(readUrl, response.data.first().highlightedScreenshotUrl)
    }

    @Test
    fun `하이라이트 스크린샷이 없는 스냅샷은 URL 을 발급하지 않음`() {
        /** given */
        val snapshot = mock<TowerSnapshot> {
            on { id } doReturn 11L
            on { createdAt } doReturn now
            on { highlightedScreenshotObjectName } doReturn null
        }
        whenever(towerSnapshotAccessor.findChangedSnapshots(tower)).thenReturn(listOf(snapshot))

        /** when */
        val response = towerService.getTowerChangedSnapshots(ownerId, towerId)

        /** then */
        assertNull(response.data.first().highlightedScreenshotUrl)
        verifyNoInteractions(snapshotStorageClient)
    }

    @Test
    fun `변경이 감지된 스냅샷이 없으면 스냅샷 목록은 비어 있음`() {
        /** given */
        whenever(towerSnapshotAccessor.findChangedSnapshots(tower))
            .thenReturn(emptyList())

        /** when */
        val response = towerService.getTowerChangedSnapshots(ownerId, towerId)

        /** then */
        assertEquals(0, response.data.size)
    }

    @Test
    fun `내부 주소를 타워로 등록하려는 요청은 회원 조회나 리소스 생성 없이 거부함`() {
        /** given */
        whenever(dnsResolver.resolveAll(towerCreateRequest.url.host))
            .thenReturn(listOf(InetAddress.ofLiteral("127.0.0.1")))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.createTower(ownerId, towerCreateRequest)
        }

        /** then */
        assertEquals(TowerError.URL_NOT_ALLOWED, exception.error)
        verifyNoInteractions(
            memberAccessor,
            towerAccessor,
            googleFaviconClient,
            notificationChannelAccessor,
            detectionConditionAccessor
        )
    }

    @Test
    fun `감지 조건이 플랜 한도를 초과하면 타워 등록 전체를 거부함`() {

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.createTower(
                ownerId, towerCreateRequest.copy(
                    conditions = List(owner.plan.maxConditionQuantity + 1) {
                        KeywordConditionCreateRequestDto(
                            keyword = "재고$it",
                            satisfiedWhen = PresenceChange.APPEAR
                        )
                    }
                ))
        }

        /** then */
        assertEquals(TowerError.CONDITION_QUOTA_EXCEEDED, exception.error)
        verify(towerAccessor, never()).saveTower(any(), any(), any())
        verifyNoInteractions(
            detectionConditionAccessor,
            googleFaviconClient
        )
    }

    @Test
    fun `회원은 키워드 조건의 단어와 감지 방향을 변경할 수 있음`() {
        /** given */
        val condition = spy(KeywordCondition.from(tower, Keyword("재고 있음"), PresenceChange.APPEAR)) {
            on { id } doReturn 3L
        }
        whenever(detectionConditionAccessor.getOwnedDetectionCondition(3L, towerId, ownerId))
            .thenReturn(condition)

        /** when */
        val response = towerService.updateDetectionCondition(
            ownerId,
            towerId,
            3L,
            KeywordConditionUpdateRequestDto(
                keyword = "품절",
                satisfiedWhen = PresenceChange.DISAPPEAR
            )
        )

        /** then */
        assertEquals(
            KeywordConditionResponseDto(
                id = 3L,
                keyword = "품절",
                satisfiedWhen = PresenceChange.DISAPPEAR
            ),
            response.data
        )
        assertEquals("품절", condition.keyword)
        assertEquals(PresenceChange.DISAPPEAR, condition.satisfiedWhen)
    }

    @Test
    fun `회원은 다른 회원의 타워에 있는 감지 조건을 수정할 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.updateDetectionCondition(
                strangerId,
                towerId,
                3L,
                KeywordConditionUpdateRequestDto(keyword = "재고", satisfiedWhen = PresenceChange.APPEAR)
            )
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verifyNoInteractions(crawlRequestProducerService)
    }

    @Test
    fun `타워에서 찾을 수 없는 감지 조건은 수정할 수 없음`() {
        /** given */
        whenever(detectionConditionAccessor.getOwnedDetectionCondition(3L, towerId, ownerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.updateDetectionCondition(
                ownerId,
                towerId,
                3L,
                KeywordConditionUpdateRequestDto(keyword = "재고", satisfiedWhen = PresenceChange.APPEAR)
            )
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }
}
