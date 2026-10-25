package kr.byeongmin.watchtower.domain.tower.controller

import kr.byeongmin.watchtower.domain.tower.dto.*
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.enums.UpdatableTowerStatus
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
        status = TowerStatus.RUNNING,
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
                            conditions = listOf(KeywordConditionCreateRequestDto("재입고", true))
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
                conditions = listOf(KeywordConditionCreateRequestDto("재입고", true))
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
            .andExpect(jsonPath("$.data.status").value(TowerStatus.RUNNING.name))
    }

    @Test
    fun `타워 이름과 수집 주기를 바꾸면서 일시정지할 수 있음`() {
        /** given */
        whenever(towerService.updateTower(eq(21L), eq(7L), any()))
            .thenReturn(
                SuccessDataResponse(
                    towerResponseDto.copy(
                        name = "배송 감시",
                        fetchInterval = Duration.ofMinutes(15),
                        includeInStat = false,
                        status = TowerStatus.PAUSED
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
                            status = UpdatableTowerStatus.PAUSED
                        )
                    )
                )
        )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("배송 감시"))
            .andExpect(jsonPath("$.data.fetchInterval").value("PT15M"))
            .andExpect(jsonPath("$.data.status").value(TowerStatus.PAUSED.name))

        verify(towerService).updateTower(
            memberId = 21L,
            towerId = 7L,
            towerUpdateRequestDto = TowerUpdateRequestDto(
                name = "배송 감시",
                fetchInterval = Duration.ofMinutes(15),
                includeInStat = false,
                status = UpdatableTowerStatus.PAUSED
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
    fun `타워에 등록된 감지 조건 목록을 조회할 수 있음`() {
        /** given */
        whenever(towerService.getTowerConditions(21L, 7L)).thenReturn(
            SuccessDataResponse(
                listOf(KeywordConditionResponseDto(id = 2L, keyword = "품절", isSatisfiedAtAppearance = false))
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
    fun `기존 타워에 정규식 감지 조건을 추가할 수 있음`() {
        /** given */
        whenever(towerService.createTowerCondition(eq(21L), eq(7L), any()))
            .thenReturn(SuccessResponse())

        /** when */
        val result = mvc.perform(
            post("/api/v1/towers/7/conditions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        RegexConditionCreateRequestDto(
                            regex = "재고: [1-9][0-9]*",
                            isSatisfiedAtAppearance = true
                        )
                    )
                )
        )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))

        verify(towerService).createTowerCondition(
            memberId = 21L,
            towerId = 7L,
            towerConditionCreateRequestDto = RegexConditionCreateRequestDto(
                regex = "재고: [1-9][0-9]*",
                isSatisfiedAtAppearance = true
            )
        )
    }

    @Test
    fun `타워에서 더 이상 사용하지 않을 감지 조건을 삭제할 수 있음`() {
        /** given */
        whenever(towerService.deleteTowerCondition(21L, 7L, 3L))
            .thenReturn(SuccessResponse())

        /** when */
        val result = mvc.perform(delete("/api/v1/towers/7/conditions/3"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))

        verify(towerService).deleteTowerCondition(memberId = 21L, towerId = 7L, conditionId = 3L)
    }

    @Test
    fun `타워의 수집 기록에서 원본과 변경 내용 및 알림 시각을 확인할 수 있음`() {
        /** given */
        whenever(towerService.getTowerSnapshots(21L, 7L)).thenReturn(
            SuccessDataResponse(
                listOf(
                    TowerSnapshotResponseDto(
                        id = 41L,
                        htmlContentUrl = "https://example.com/snapshots/41.html",
                        htmlContentDiffUrl = "https://example.com/snapshots/41.diff.html",
                        screenshotUrl = "https://example.com/snapshots/41.png",
                        screenshotDiffUrl = "https://example.com/snapshots/41.diff.png",
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
            .andExpect(jsonPath("$.data[0].htmlContentUrl").value("https://example.com/snapshots/41.html"))
            .andExpect(jsonPath("$.data[0].htmlContentDiffUrl").value("https://example.com/snapshots/41.diff.html"))
            .andExpect(jsonPath("$.data[0].notifiedAt").value("2026-10-20T12:35:06"))

    }

    @Test
    fun `타워의 키워드 조건에서 감지할 단어와 감지 방향을 변경할 수 있음`() {
        /** given */
        whenever(towerService.updateTowerCondition(eq(21L), eq(7L), eq(3L), any()))
            .thenReturn(
                SuccessDataResponse(
                    KeywordConditionResponseDto(
                        id = 3L,
                        keyword = "품절",
                        isSatisfiedAtAppearance = false
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
                                isSatisfiedAtAppearance = false
                            )
                        )
                    )
            )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.keyword").value("품절"))
            .andExpect(jsonPath("$.data.isSatisfiedAtAppearance").value(false))

        verify(towerService).updateTowerCondition(
            memberId = 21L,
            towerId = 7L,
            conditionId = 3L,
            towerConditionUpdateRequestDto = KeywordConditionUpdateRequestDto(
                keyword = "품절",
                isSatisfiedAtAppearance = false
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
                            changeCount = 12
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
            .andExpect(jsonPath("$.data[0].changeCount").value(12))

        verifyNoInteractions(towerService)
    }
}
