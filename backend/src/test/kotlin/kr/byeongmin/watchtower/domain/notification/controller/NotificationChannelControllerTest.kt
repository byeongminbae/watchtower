package kr.byeongmin.watchtower.domain.notification.controller

import tools.jackson.module.kotlin.jacksonObjectMapper
import kr.byeongmin.watchtower.domain.notification.dto.NotificationChannelCreateRequestDto
import kr.byeongmin.watchtower.domain.notification.dto.NotificationChannelResponseDto
import kr.byeongmin.watchtower.domain.notification.dto.NotificationChannelUpdateRequestDto
import kr.byeongmin.watchtower.domain.notification.enums.NotificationChannelType
import kr.byeongmin.watchtower.domain.notification.service.NotificationChannelService
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.CursorPaginationResponseDto
import kr.byeongmin.watchtower.global.security.MemberPrinciple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class NotificationChannelControllerTest {
    private val objectMapper = jacksonObjectMapper()

    private val service = mock<NotificationChannelService>()
    private val controller = NotificationChannelController(service)
    private val mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
        .build()
    private val channelResponseDto = NotificationChannelResponseDto(
        id = 31L,
        name = "개인 알림",
        type = NotificationChannelType.DISCORD,
        webhookUrl = "https://example.com/hook"
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
    fun `로그인한 회원의 알림 채널을 등록할 수 있음`() {
        /** given */
        whenever(service.createChannel(eq(21L), any())).thenReturn(
            SuccessDataResponse(channelResponseDto)
        )

        /** when */
        val result = mockMvc
            .perform(
                post("/api/v1/notification/channels")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsBytes(
                            NotificationChannelCreateRequestDto(
                                name = "개인 알림",
                                type = NotificationChannelType.DISCORD,
                                webhookUrl = "https://example.com/hook"
                            )
                        )
                    )
            )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(31))

        verify(service).createChannel(
            memberId = 21L,
            notificationChannelCreateRequestDto = NotificationChannelCreateRequestDto(
                name = "개인 알림",
                type = NotificationChannelType.DISCORD,
                webhookUrl = "https://example.com/hook"
            )
        )
    }

    @Test
    fun `로그인한 회원에게 등록된 알림 채널 목록을 조회할 수 있음`() {
        /** given */
        whenever(service.getChannels(eq(21L), any(), eq(BaseSortField.UPDATED_AT))).thenReturn(
            SuccessDataResponse(
                CursorPaginationResponseDto(
                    data = listOf(channelResponseDto),
                    size = 1,
                    nextCursorId = 31L,
                    nextCursorValue = null,
                    hasNext = true
                )
            )
        )

        /** when */
        val result = mockMvc.perform(
            get("/api/v1/notification/channels")
                .param("query", "개인")
                .param("cursorId", "30")
                .param("cursorValue", "2026-10-22T12:00")
                .param("size", "1")
                .param("sortDirection", Sort.Direction.ASC.name)
                .param("sortField", BaseSortField.UPDATED_AT.name)
        )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.data[0].id").value(31))

        verify(service).getChannels(
            memberId = 21L,
            cursorPaginationRequestDto = CursorPaginationRequestDto(
                query = "개인",
                cursorId = 30L,
                cursorValue = "2026-10-22T12:00",
                size = 1,
                sortDirection = Sort.Direction.ASC
            ),
            sortField = BaseSortField.UPDATED_AT
        )
    }

    @Test
    fun `등록한 알림 채널의 이름과 웹훅 주소를 조회할 수 있음`() {
        /** given */
        whenever(service.getChannel(21L, 31L)).thenReturn(SuccessDataResponse(channelResponseDto))

        /** when */
        val result = mockMvc.perform(get("/api/v1/notification/channels/31"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("개인 알림"))
            .andExpect(jsonPath("$.data.webhookUrl").value("https://example.com/hook"))

    }

    @Test
    fun `알림을 받을 채널의 이름과 웹훅 주소를 변경할 수 있음`() {
        /** given */
        whenever(service.updateChannel(eq(21L), eq(31L), any())).thenReturn(
            SuccessDataResponse(
                channelResponseDto.copy(
                    name = "업무 알림",
                    webhookUrl = "https://example.com/work-hook"
                )
            )
        )

        /** when */
        val result = mockMvc.perform(
            put("/api/v1/notification/channels/31")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        NotificationChannelUpdateRequestDto(
                            name = "업무 알림",
                            webhookUrl = "https://example.com/work-hook"
                        )
                    )
                )
        )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.name").value("업무 알림"))
            .andExpect(jsonPath("$.data.webhookUrl").value("https://example.com/work-hook"))

        verify(service).updateChannel(
            memberId = 21L,
            channelId = 31L,
            request = NotificationChannelUpdateRequestDto(
                name = "업무 알림",
                webhookUrl = "https://example.com/work-hook"
            )
        )
    }

    @Test
    fun `등록한 알림 채널을 삭제할 수 있음`() {
        /** given */
        whenever(service.deleteChannel(21L, 31L)).thenReturn(SuccessResponse())

        /** when */
        val result = mockMvc.perform(delete("/api/v1/notification/channels/31"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))

        verify(service).deleteChannel(memberId = 21L, channelId = 31L)
    }
}
