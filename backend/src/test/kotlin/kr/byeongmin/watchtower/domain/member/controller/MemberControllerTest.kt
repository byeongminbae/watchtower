package kr.byeongmin.watchtower.domain.member.controller

import kr.byeongmin.watchtower.domain.member.dto.MemberResponseDto
import kr.byeongmin.watchtower.domain.member.dto.MemberUpdateRequestDto
import kr.byeongmin.watchtower.domain.member.service.MemberService
import kr.byeongmin.watchtower.domain.tower.dto.TowerListItemResponseDto
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.CursorPaginationResponseDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.time.LocalDateTime

class MemberControllerTest {
    private val objectMapper = jacksonObjectMapper()
    private val memberService = mock<MemberService>()
    private val mockMvc = MockMvcBuilders.standaloneSetup(MemberController(memberService)).build()
    private val memberResponseDto = MemberResponseDto(
        id = 21L,
        email = "member@example.com",
        nickname = "회원",
        profileImageUrl = "https://example.com/profile",
        lastSignInAt = LocalDateTime.of(2026, 10, 20, 12, 34, 56)
    )

    @Test
    fun `프로필에서 이메일과 닉네임 및 최근 로그인 시각을 확인할 수 있음`() {
        /** given */
        whenever(memberService.getMember(21L)).thenReturn(SuccessDataResponse(memberResponseDto))

        /** when */
        val result = mockMvc.perform(get("/api/v1/member/21"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.email").value("member@example.com"))
            .andExpect(jsonPath("$.data.nickname").value("회원"))
            .andExpect(jsonPath("$.data.lastSignInAt").value("2026-10-20T12:34:56"))

    }

    @Test
    fun `회원의 타워 목록에서 타워 정보와 AI 요약을 확인할 수 있음`() {
        /** given */
        whenever(memberService.getMemberTowers(eq(21L), any(), eq(BaseSortField.CREATED_AT))).thenReturn(
            SuccessDataResponse(
                CursorPaginationResponseDto(
                    data = listOf(
                        TowerListItemResponseDto(
                            id = 31L,
                            name = "재고 감시",
                            url = "https://example.com/products",
                            fetchInterval = Duration.ofMinutes(10),
                            includeInStat = true,
                            status = TowerStatus.RUNNING,
                            faviconUrl = "https://example.com/favicon.ico",
                            lastFetchedAt = null,
                            lastNotifiedAt = null,
                            latestAiSummary = "재입고됨"
                        )
                    ),
                    size = 1,
                    nextCursorId = 31L,
                    nextCursorValue = "2026-10-22T12:01",
                    hasNext = true
                )
            )
        )

        /** when */
        val result = mockMvc.perform(
            get("/api/v1/member/21/towers")
                .param("query", "재고")
                .param("cursorId", "30")
                .param("cursorValue", "2026-10-22T12:00")
                .param("size", "1")
                .param("sortDirection", Sort.Direction.ASC.name)
                .param("sortField", BaseSortField.CREATED_AT.name)
        )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.data[0].id").value(31))
            .andExpect(jsonPath("$.data.data[0].latestAiSummary").value("재입고됨"))

        verify(memberService).getMemberTowers(
            memberId = 21L,
            cursorPaginationRequestDto = CursorPaginationRequestDto(
                query = "재고",
                cursorId = 30L,
                cursorValue = "2026-10-22T12:00",
                size = 1,
                sortDirection = Sort.Direction.ASC
            ),
            sortField = BaseSortField.CREATED_AT
        )
    }

    @Test
    fun `프로필의 닉네임과 이미지를 변경하면 바뀐 프로필을 받을 수 있음`() {
        /** given */
        whenever(memberService.updateMember(eq(21L), any())).thenReturn(
            SuccessDataResponse(
                memberResponseDto.copy(
                    nickname = "새 닉네임",
                    profileImageUrl = "https://example.com/new-profile"
                )
            )
        )

        /** when */
        val result = mockMvc.perform(
            put("/api/v1/member/21")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        MemberUpdateRequestDto(
                            nickname = "새 닉네임",
                            profileImageUrl = "https://example.com/new-profile"
                        )
                    )
                )
        )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.nickname").value("새 닉네임"))
            .andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/new-profile"))

        verify(memberService).updateMember(
            memberId = 21L,
            memberUpdateRequestDto = MemberUpdateRequestDto(
                nickname = "새 닉네임",
                profileImageUrl = "https://example.com/new-profile"
            )
        )
    }

    @Test
    fun `회원 탈퇴를 요청할 수 있음`() {
        /** given */
        whenever(memberService.deleteMember(21L)).thenReturn(SuccessResponse())

        /** when */
        val result = mockMvc.perform(delete("/api/v1/member/21"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))

        verify(memberService).deleteMember(21L)
    }
}
