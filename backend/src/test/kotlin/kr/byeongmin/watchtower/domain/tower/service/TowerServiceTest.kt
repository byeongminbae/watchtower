package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.notification.accessor.NotificationChannelAccessor
import kr.byeongmin.watchtower.domain.notification.entity.NotificationChannel
import kr.byeongmin.watchtower.domain.notification.enums.NotificationChannelType
import kr.byeongmin.watchtower.domain.subscription.entity.Plan
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerConditionAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.dto.*
import kr.byeongmin.watchtower.domain.tower.entity.*
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.enums.UpdatableTowerStatus
import kr.byeongmin.watchtower.domain.tower.error.TowerError
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.external.dns.DnsResolver
import kr.byeongmin.watchtower.external.google.GoogleFaviconClient
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import java.net.InetAddress
import java.time.Duration
import kotlin.test.*

class TowerServiceTest {
    private val towerAccessor = mock<TowerAccessor>()
    private val towerConditionAccessor = mock<TowerConditionAccessor>()
    private val towerSnapshotAccessor = mock<TowerSnapshotAccessor>()
    private val memberAccessor = mock<MemberAccessor>()
    private val notificationChannelAccessor = mock<NotificationChannelAccessor>()
    private val googleFaviconClient = mock<GoogleFaviconClient>()
    private val dnsResolver = mock<DnsResolver> {
        on { resolveAll(any()) } doReturn listOf(InetAddress.ofLiteral("8.8.8.8"))
    }

    private val towerService = TowerService(
        towerAccessor = towerAccessor,
        towerConditionAccessor = towerConditionAccessor,
        towerSnapshotAccessor = towerSnapshotAccessor,
        memberAccessor = memberAccessor,
        notificationChannelAccessor = notificationChannelAccessor,
        googleFaviconClient = googleFaviconClient,
        dnsResolver = dnsResolver
    )

    private val ownerId = 21L
    private val strangerId = 99L
    private val towerId = 7L
    private val plan = mock<Plan> {
        on { maxConditionQuantity } doReturn 3
    }
    private val owner = mock<Member> {
        on { plan } doReturn plan
        on { isTowerAddable(any()) } doReturn true
    }
    private val towerCreateRequest = TowerCreateRequestDto(
        name = "타워",
        url = TowerUrl.from("https://byeongmin.kr/notice"),
        fetchInterval = Duration.ofMinutes(10),
        includeInStat = true,
        notificationChannelIds = emptySet(),
        conditions = listOf(HtmlConditionCreateRequestDto)
    )
    private val towerUpdateRequest = TowerUpdateRequestDto(
        name = "타워",
        fetchInterval = Duration.ofMinutes(10),
        includeInStat = true,
        status = UpdatableTowerStatus.RUNNING
    )
    private val tower = spy(Tower.from(owner, towerCreateRequest)) {
        on { id } doReturn towerId
    }

    @BeforeEach
    fun setUp() {
        whenever(memberAccessor.getMemberForUpdate(ownerId))
            .thenReturn(owner)
        whenever(towerAccessor.getOwnedTower(towerId, ownerId))
            .thenReturn(tower)
        whenever(towerAccessor.getOwnedTower(towerId, strangerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))
        whenever(towerConditionAccessor.getOwnedCondition(3L, towerId, strangerId))
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
    fun `타워를 등록할 때 선택한 알림 채널을 연결할 수 있음`() {
        /** given */
        val channelId = 31L
        val channel = NotificationChannel.from(
            member = owner,
            name = "개인 디스코드",
            type = NotificationChannelType.DISCORD,
            webhookUrl = "https://discord.com/api/webhooks/example"
        )
        whenever(notificationChannelAccessor.findByIdsAndMemberId(setOf(channelId), ownerId))
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
        whenever(notificationChannelAccessor.findByIdsAndMemberId(request.notificationChannelIds, ownerId))
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
        verifyNoInteractions(towerConditionAccessor)
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
        assertEquals(TowerStatus.RUNNING, response.data.status)
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
    fun `회원은 자신의 타워 이름과 수집 주기 및 실행 상태를 변경할 수 있음`() {
        /** when */
        val response = towerService.updateTower(
            ownerId, towerId, towerUpdateRequest.copy(
                name = "변경된-타워-이름",
                fetchInterval = Duration.ofSeconds(1800),
                includeInStat = false,
                status = UpdatableTowerStatus.PAUSED
            )
        )

        /** then */
        assertEquals("변경된-타워-이름", response.data.name)
        assertEquals("https://byeongmin.kr/notice", response.data.url)
        assertEquals(Duration.ofSeconds(1800), response.data.fetchInterval)
        assertEquals(false, response.data.includeInStat)
        assertEquals(TowerStatus.PAUSED, response.data.status)
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
    fun `타워 상태 변경이 거부되면 아무것도 변경하지 않음`() {
        /** given */
        val rejection = BusinessException(CommonError.PERMISSION_DENIED)
        doThrow(rejection).whenever(tower).changeStatusByMember(UpdatableTowerStatus.RUNNING)

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
        verify(tower, never()).rename(any())
        verify(tower, never()).changeFetchInterval(any())
        verify(tower, never()).changeStatInclusion(any())
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
        whenever(towerConditionAccessor.countByTower(tower))
            .thenReturn(2L)

        /** when */
        towerService.createTowerCondition(
            ownerId, towerId, KeywordConditionCreateRequestDto(
                keyword = "재고 있음",
                isSatisfiedAtAppearance = true
            )
        )

        /** then */
        val savedCondition = argumentCaptor<KeywordCondition>()
        verify(towerConditionAccessor).save(savedCondition.capture())
        assertEquals("재고 있음", savedCondition.firstValue.keyword)
        assertEquals(true, savedCondition.firstValue.isSatisfiedAtAppearance)
    }

    @ParameterizedTest
    @ValueSource(longs = [3, 4])
    fun `감지 조건이 플랜 한도에 도달하면 조건을 더 추가할 수 없음(경계값 테스트)`(conditionCount: Long) {
        /** given */
        whenever(towerConditionAccessor.countByTower(tower))
            .thenReturn(conditionCount)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.createTowerCondition(
                ownerId, towerId, KeywordConditionCreateRequestDto(
                    keyword = "재고 있음",
                    isSatisfiedAtAppearance = false
                )
            )
        }

        /** then */
        assertEquals(TowerError.CONDITION_QUOTA_EXCEEDED, exception.error)
        verify(towerConditionAccessor, never()).save(any<KeywordCondition>())
    }

    @Test
    fun `감지 조건 생성에 실패하면 조건을 저장하지 않음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.createTowerCondition(
                ownerId, towerId, RegexConditionCreateRequestDto(
                    regex = "",
                    isSatisfiedAtAppearance = true
                )
            )
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, exception.error)
        verify(towerConditionAccessor, never()).save(any<RegexCondition>())
    }

    @Test
    fun `등록한 감지 조건의 종류와 감지할 내용을 확인할 수 있음`() {
        /** given */
        val regexCondition = mock<RegexCondition> {
            on { id } doReturn 1L
            on { regex } doReturn "재고\\s*있음"
            on { isSatisfiedAtAppearance } doReturn true
        }
        val htmlCondition = mock<HtmlCondition> { on { id } doReturn 2L }
        whenever(towerConditionAccessor.getOwnedTowerConditions(towerId, ownerId))
            .thenReturn(listOf(regexCondition, htmlCondition))

        /** when */
        val response = towerService.getTowerConditions(ownerId, towerId)

        /** then */
        val regexResponse = assertIs<RegexConditionResponseDto>(response.data.first())
        assertEquals("재고\\s*있음", regexResponse.regex)
        assertEquals(true, regexResponse.isSatisfiedAtAppearance)
        assertIs<HtmlConditionResponseDto>(response.data.last())
    }

    @Test
    fun `소유한 타워에 감지 조건이 없으면 조건 목록은 비어 있음`() {
        /** given */
        whenever(towerConditionAccessor.getOwnedTowerConditions(towerId, ownerId))
            .thenReturn(emptyList())

        /** when */
        val response = towerService.getTowerConditions(ownerId, towerId)

        /** then */
        assertTrue(response.data.isEmpty())
    }

    @Test
    fun `회원은 다른 회원의 타워에 있는 감지 조건을 조회할 수 없음`() {
        /** given */
        whenever(towerConditionAccessor.getOwnedTowerConditions(towerId, strangerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.getTowerConditions(strangerId, towerId)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `회원은 정규식 조건의 패턴과 감지 방향을 변경할 수 있음`() {
        /** given */
        val conditionId = 3L
        val condition = spy(RegexCondition.from(tower, "기존", true)) {
            on { id } doReturn conditionId
        }
        whenever(towerConditionAccessor.getOwnedCondition(conditionId, towerId, ownerId))
            .thenReturn(condition)

        /** when */
        val response = towerService.updateTowerCondition(
            ownerId,
            towerId,
            conditionId,
            RegexConditionUpdateRequestDto(
                regex = "품절",
                isSatisfiedAtAppearance = false
            )
        )

        /** then */
        assertEquals("품절", assertIs<RegexConditionResponseDto>(response.data).regex)
        assertEquals("품절", condition.regex)
        assertEquals(false, condition.isSatisfiedAtAppearance)
    }

    @Test
    fun `키워드 감지 조건을 정규식 조건으로 변경할 수 없음`() {
        /** given */
        val condition = KeywordCondition.from(tower, "재고 있음", true)
        val conditionId = 3L
        whenever(towerConditionAccessor.getOwnedCondition(conditionId, towerId, ownerId))
            .thenReturn(condition)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.updateTowerCondition(
                ownerId,
                towerId,
                conditionId,
                RegexConditionUpdateRequestDto(
                    regex = "품절",
                    isSatisfiedAtAppearance = true
                )
            )
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `타워에서 찾을 수 없는 감지 조건은 삭제할 수 없음`() {
        /** given */
        val conditionId = 404L
        whenever(towerConditionAccessor.getOwnedCondition(conditionId, towerId, ownerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.deleteTowerCondition(ownerId, towerId, conditionId)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `감지 조건 삭제를 요청하면 소유한 조건을 찾아 삭제 처리함`() {
        /** given */
        val conditionId = 3L
        val condition = mock<KeywordCondition>()
        whenever(towerConditionAccessor.getOwnedCondition(conditionId, towerId, ownerId))
            .thenReturn(condition)

        /** when */
        towerService.deleteTowerCondition(ownerId, towerId, conditionId)

        /** then */
        verify(condition).delete()
    }

    @Test
    fun `소유한 타워를 찾을 수 없으면 스냅샷을 조회할 수 없음`() {
        /** given */
        whenever(towerSnapshotAccessor.getMemberSnapshots(towerId, ownerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.getTowerSnapshots(ownerId, towerId)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `회원은 자신의 타워에 저장된 스냅샷을 조회할 수 있음`() {
        /** given */
        val snapshotId = 11L
        val snapshot = mock<TowerSnapshot> {
            on { id } doReturn snapshotId
            on { htmlContentUrl } doReturn "https://example.com/snapshot"
        }
        whenever(towerSnapshotAccessor.getMemberSnapshots(towerId, ownerId))
            .thenReturn(listOf(snapshot))

        /** when */
        val response = towerService.getTowerSnapshots(ownerId, towerId)

        /** then */
        assertEquals(1, response.data.size)
        assertEquals(snapshotId, response.data.first().id)
    }

    @Test
    fun `조회할 스냅샷이 없으면 스냅샷 목록은 비어 있음`() {
        /** given */
        whenever(towerSnapshotAccessor.getMemberSnapshots(towerId, ownerId))
            .thenReturn(emptyList())

        /** when */
        val response = towerService.getTowerSnapshots(ownerId, towerId)

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
            towerConditionAccessor
        )
    }

    @Test
    fun `감지 조건이 플랜 한도를 초과하면 타워 등록 전체를 거부함`() {

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.createTower(
                ownerId, towerCreateRequest.copy(
                    conditions = List(owner.plan.maxConditionQuantity + 1) { HtmlConditionCreateRequestDto }
                ))
        }

        /** then */
        assertEquals(TowerError.CONDITION_QUOTA_EXCEEDED, exception.error)
        verify(towerAccessor, never()).saveTower(any(), any(), any())
        verifyNoInteractions(
            towerConditionAccessor,
            googleFaviconClient
        )
    }

    @Test
    fun `회원은 키워드 조건의 단어와 감지 방향을 변경할 수 있음`() {
        /** given */
        val condition = spy(KeywordCondition.from(tower, "재고 있음", true)) {
            on { id } doReturn 3L
        }
        whenever(towerConditionAccessor.getOwnedCondition(3L, towerId, ownerId))
            .thenReturn(condition)

        /** when */
        val response = towerService.updateTowerCondition(
            ownerId,
            towerId,
            3L,
            KeywordConditionUpdateRequestDto(
                keyword = "품절",
                isSatisfiedAtAppearance = false
            )
        )

        /** then */
        assertEquals(
            KeywordConditionResponseDto(
                id = 3L,
                keyword = "품절",
                isSatisfiedAtAppearance = false
            ),
            response.data
        )
        assertEquals("품절", condition.keyword)
        assertEquals(false, condition.isSatisfiedAtAppearance)
    }

    @Test
    fun `정규식 조건을 키워드 조건으로 변경할 수 없음`() {
        /** given */
        val condition = RegexCondition.from(tower, "기존", true)
        whenever(towerConditionAccessor.getOwnedCondition(3L, towerId, ownerId))
            .thenReturn(condition)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.updateTowerCondition(
                ownerId,
                towerId,
                3L,
                KeywordConditionUpdateRequestDto(
                    keyword = "품절",
                    isSatisfiedAtAppearance = false
                )
            )
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        assertEquals("기존", condition.regex)
        assertTrue(condition.isSatisfiedAtAppearance)
    }

    @Test
    fun `정규식을 잘못된 패턴으로 바꾸려 해도 기존 패턴과 감지 방향은 유지됨`() {
        /** given */
        val condition = RegexCondition.from(tower, "기존", true)
        whenever(towerConditionAccessor.getOwnedCondition(3L, towerId, ownerId))
            .thenReturn(condition)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.updateTowerCondition(
                ownerId,
                towerId,
                3L,
                RegexConditionUpdateRequestDto(
                    regex = "[",
                    isSatisfiedAtAppearance = false
                )
            )
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, exception.error)
        assertEquals("기존", condition.regex)
        assertTrue(condition.isSatisfiedAtAppearance)
    }

    @Test
    fun `HTML 감지 조건을 다른 조건으로 수정할 수 없음`() {
        /** given */
        val condition = HtmlCondition.from(tower)
        whenever(towerConditionAccessor.getOwnedCondition(3L, towerId, ownerId))
            .thenReturn(condition)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.updateTowerCondition(
                ownerId,
                towerId,
                3L,
                RegexConditionUpdateRequestDto(
                    regex = "재고",
                    isSatisfiedAtAppearance = true
                )
            )
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `회원은 다른 회원의 타워에 있는 감지 조건을 수정할 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.updateTowerCondition(
                strangerId,
                towerId,
                3L,
                RegexConditionUpdateRequestDto(
                    regex = "재고",
                    isSatisfiedAtAppearance = true
                )
            )
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verify(towerConditionAccessor).getOwnedCondition(3L, towerId, strangerId)
    }

    @Test
    fun `타워에서 찾을 수 없는 감지 조건은 수정할 수 없음`() {
        /** given */
        whenever(towerConditionAccessor.getOwnedCondition(3L, towerId, ownerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerService.updateTowerCondition(
                ownerId,
                towerId,
                3L,
                RegexConditionUpdateRequestDto(
                    regex = "재고",
                    isSatisfiedAtAppearance = true
                )
            )
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }
}
