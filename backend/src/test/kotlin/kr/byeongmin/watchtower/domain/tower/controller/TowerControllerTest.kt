package kr.byeongmin.watchtower.domain.tower.controller

import kr.byeongmin.watchtower.domain.tower.dto.*
import kr.byeongmin.watchtower.domain.tower.enums.PresenceChange
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.service.HotTowerService
import kr.byeongmin.watchtower.domain.tower.service.TowerService
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.security.MemberPrinciple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.time.LocalDateTime

class TowerControllerTest {
    private val objectMapper = jacksonObjectMapper()

    private val towerService = mock<TowerService>()
    private val hotService = mock<HotTowerService>()
    private val mvc = MockMvcBuilders
        .standaloneSetup(TowerController(towerService, hotService))
        .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
        .build()
    private val towerResponseDto = TowerResponseDto(
        id = 7L,
        name = "재고 감시",
        url = "https://example.com/products",
        fetchInterval = Duration.ofMinutes(10),
        includeInStat = true,
        detectTextChange = true,
        detectImageChange = false,
        ignoreDateTime = false,
        detectionAreaSelector = null,
        status = TowerStatus.READY,
        faviconUrl = "https://example.com/favicon.ico",
        lastFetchedAt = LocalDateTime.of(2026, 10, 20, 12, 34, 56),
        lastNotifiedAt = LocalDateTime.of(2026, 10, 20, 12, 35, 6)
    )

    @BeforeEach
    fun authenticate() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            MemberPrinciple(21L, "NORMAL"),
            null,
            emptyList()
        )
    }

    @AfterEach
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `로그인한 회원의 타워 등록 요청을 처리할 수 있음`() {
        /** given */
        whenever(towerService.createTower(eq(21L), any()))
            .thenReturn(SuccessResponse())

        /** when */
        val result = mvc.perform(
            post("/api/v1/towers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        TowerCreateRequestDto(
                            name = "재고",
                            url = TowerUrl.from("https://example.com"),
                            fetchInterval = Duration.ofMinutes(10),
                            includeInStat = true,
                            notificationChannelIds = setOf(31L),
                            conditions = listOf(KeywordConditionCreateRequestDto("재입고", PresenceChange.APPEAR))
                        )
                    )
                )
        )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))

        verify(towerService).createTower(
            memberId = 21L,
            towerCreateRequestDto = TowerCreateRequestDto(
                name = "재고",
                url = TowerUrl.from("https://example.com"),
                fetchInterval = Duration.ofMinutes(10),
                includeInStat = true,
                notificationChannelIds = setOf(31L),
                conditions = listOf(KeywordConditionCreateRequestDto("재입고", PresenceChange.APPEAR))
            )
        )
    }

    @Test
    fun `타워 상세에서 수집 주기와 운영 상태를 확인할 수 있음`() {
        /** given */
        whenever(towerService.getTower(21L, 7L))
            .thenReturn(SuccessDataResponse(towerResponseDto))

        /** when */
        val result = mvc.perform(get("/api/v1/towers/7"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(7))
            .andExpect(jsonPath("$.data.fetchInterval").value("PT10M"))
            .andExpect(jsonPath("$.data.status").value(TowerStatus.READY.name))
    }

    @Test
    fun `타워 이름과 수집 주기를 바꿀 수 있음`() {
        /** given */
        whenever(towerService.updateTower(eq(21L), eq(7L), any()))
            .thenReturn(
                SuccessDataResponse(
                    towerResponseDto.copy(
                        name = "배송 감시",
                        fetchInterval = Duration.ofMinutes(15),
                        includeInStat = false,
                        detectTextChange = true,
                        detectImageChange = false,
                        ignoreDateTime = false,
                        detectionAreaSelector = null
                    )
                )
            )

        /** when */
        val result = mvc.perform(
            put("/api/v1/towers/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        TowerUpdateRequestDto(
                            name = "배송 감시",
                            fetchInterval = Duration.ofMinutes(15),
                            includeInStat = false,
                            detectTextChange = true,
                            detectImageChange = true,
                            ignoreDateTime = true,
                            detectionAreaSelector = null
                        )
                    )
                )
        )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("배송 감시"))
            .andExpect(jsonPath("$.data.fetchInterval").value("PT15M"))

        verify(towerService).updateTower(
            memberId = 21L,
            towerId = 7L,
            towerUpdateRequestDto = TowerUpdateRequestDto(
                name = "배송 감시",
                fetchInterval = Duration.ofMinutes(15),
                includeInStat = false,
                detectTextChange = true,
                detectImageChange = true,
                ignoreDateTime = true,
                detectionAreaSelector = null
            )
        )
    }

    @Test
    fun `타워를 삭제할 수 있음`() {
        /** given */
        whenever(towerService.deleteTower(21L, 7L))
            .thenReturn(SuccessResponse())

        /** when */
        val result = mvc.perform(delete("/api/v1/towers/7"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))

        verify(towerService).deleteTower(memberId = 21L, towerId = 7L)
    }

    @Test
    fun `선택한 타워에 테스트 알림을 요청할 수 있음`() {
        /** given */
        whenever(towerService.sendTestNotification(21L, 7L))
            .thenReturn(SuccessResponse())

        /** when */
        val result = mvc.perform(post("/api/v1/towers/7/notify"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))

        verify(towerService).sendTestNotification(memberId = 21L, towerId = 7L)
    }

    @Test
    fun `크롤링에 실패한 타워를 재시도하면 바뀐 타워 상태를 응답함`() {
        /** given */
        whenever(towerService.retryCrawl(21L, 7L))
            .thenReturn(SuccessDataResponse(towerResponseDto.copy(status = TowerStatus.RUNNING)))

        /** when */
        val result = mvc.perform(post("/api/v1/towers/7/crawl-retry"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value(TowerStatus.RUNNING.name))

        verify(towerService).retryCrawl(memberId = 21L, towerId = 7L)
    }

    @Test
    fun `타워를 일시정지하면 바뀐 타워 상태를 응답함`() {
        /** given */
        whenever(towerService.pauseTower(21L, 7L))
            .thenReturn(SuccessDataResponse(towerResponseDto.copy(status = TowerStatus.PAUSED)))

        /** when */
        val result = mvc.perform(post("/api/v1/towers/7/pause"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value(TowerStatus.PAUSED.name))

        verify(towerService).pauseTower(memberId = 21L, towerId = 7L)
    }

    @Test
    fun `일시정지한 타워를 다시 시작하면 바뀐 타워 상태를 응답함`() {
        /** given */
        whenever(towerService.resumeTower(21L, 7L))
            .thenReturn(SuccessDataResponse(towerResponseDto.copy(status = TowerStatus.READY)))

        /** when */
        val result = mvc.perform(post("/api/v1/towers/7/resume"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value(TowerStatus.READY.name))

        verify(towerService).resumeTower(memberId = 21L, towerId = 7L)
    }

    @Test
    fun `타워에 등록된 감지 조건 목록을 조회할 수 있음`() {
        /** given */
        whenever(towerService.getDetectionConditions(21L, 7L)).thenReturn(
            SuccessDataResponse(
                listOf(KeywordConditionResponseDto(id = 2L, keyword = "품절", satisfiedWhen = PresenceChange.DISAPPEAR))
            )
        )

        /** when */
        val result = mvc.perform(get("/api/v1/towers/7/conditions"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(2))

    }

    @Test
    fun `기존 타워에 키워드 감지 조건을 추가할 수 있음`() {
        /** given */
        whenever(towerService.createDetectionCondition(eq(21L), eq(7L), any()))
            .thenReturn(SuccessResponse())

        /** when */
        val result = mvc.perform(
            post("/api/v1/towers/7/conditions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        KeywordConditionCreateRequestDto(keyword = "재입고", satisfiedWhen = PresenceChange.APPEAR)
                    )
                )
        )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))

        verify(towerService).createDetectionCondition(
            memberId = 21L,
            towerId = 7L,
            detectionConditionCreateRequestDto = KeywordConditionCreateRequestDto(keyword = "재입고", satisfiedWhen = PresenceChange.APPEAR)
        )
    }

    @Test
    fun `타워에서 더 이상 사용하지 않을 감지 조건을 삭제할 수 있음`() {
        /** given */
        whenever(towerService.deleteDetectionCondition(21L, 7L, 3L))
            .thenReturn(SuccessResponse())

        /** when */
        val result = mvc.perform(delete("/api/v1/towers/7/conditions/3"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))

        verify(towerService).deleteDetectionCondition(memberId = 21L, towerId = 7L, conditionId = 3L)
    }

    @Test
    fun `타워의 변경 기록에서 감지 시각과 하이라이트 스크린샷 URL 과 변경 요약 및 알림 시각을 확인할 수 있음`() {
        /** given */
        whenever(towerService.getTowerChangedSnapshots(21L, 7L)).thenReturn(
            SuccessDataResponse(
                listOf(
                    TowerSnapshotResponseDto(
                        id = 41L,
                        detectedAt = LocalDateTime.of(2026, 10, 20, 12, 30, 1),
                        highlightedScreenshotUrl = "https://objectstorage.ap-osaka-1.oraclecloud.com/p/token/o/41.png",
                        aiDiffSummary = "상품이 재입고됨",
                        notifiedAt = LocalDateTime.of(2026, 10, 20, 12, 35, 6)
                    )
                )
            )
        )

        /** when */
        val result = mvc.perform(get("/api/v1/towers/7/snapshots"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(41))
            .andExpect(jsonPath("$.data[0].detectedAt").value("2026-10-20T12:30:01"))
            .andExpect(jsonPath("$.data[0].highlightedScreenshotUrl").value("https://objectstorage.ap-osaka-1.oraclecloud.com/p/token/o/41.png"))
            .andExpect(jsonPath("$.data[0].aiDiffSummary").value("상품이 재입고됨"))
            .andExpect(jsonPath("$.data[0].notifiedAt").value("2026-10-20T12:35:06"))

    }

    @Test
    fun `타워의 키워드 조건에서 감지할 단어와 감지 방향을 변경할 수 있음`() {
        /** given */
        whenever(towerService.updateDetectionCondition(eq(21L), eq(7L), eq(3L), any()))
            .thenReturn(
                SuccessDataResponse(
                    KeywordConditionResponseDto(
                        id = 3L,
                        keyword = "품절",
                        satisfiedWhen = PresenceChange.DISAPPEAR
                    )
                )
            )

        /** when */
        val result = mvc
            .perform(
                put("/api/v1/towers/7/conditions/3")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsBytes(
                            KeywordConditionUpdateRequestDto(
                                keyword = "품절",
                                satisfiedWhen = PresenceChange.DISAPPEAR
                            )
                        )
                    )
            )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.keyword").value("품절"))
            .andExpect(jsonPath("$.data.satisfiedWhen").value(PresenceChange.DISAPPEAR.name))

        verify(towerService).updateDetectionCondition(
            memberId = 21L,
            towerId = 7L,
            conditionId = 3L,
            detectionConditionUpdateRequestDto = KeywordConditionUpdateRequestDto(
                keyword = "품절",
                satisfiedWhen = PresenceChange.DISAPPEAR
            )
        )
    }

    @Test
    fun `인기 타워 목록에서 사이트의 변경 횟수를 확인할 수 있음`() {
        /** given */
        whenever(hotService.getTrendingTowers())
            .thenReturn(
                SuccessDataResponse(
                    listOf(
                        HotTowerResponseDto(
                            domain = "example.com",
                            url = "https://example.com",
                            detectionCount = 12
                        )
                    )
                )
            )

        /** when */
        val result = mvc
            .perform(get("/api/v1/towers/trending"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].detectionCount").value(12))

        verifyNoInteractions(towerService)
    }
}
