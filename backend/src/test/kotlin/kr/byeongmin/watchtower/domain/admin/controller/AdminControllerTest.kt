package kr.byeongmin.watchtower.domain.admin.controller

import kr.byeongmin.watchtower.domain.admin.dto.MemberResponseDto
import kr.byeongmin.watchtower.domain.admin.dto.MemberRoleUpdateRequestDto
import kr.byeongmin.watchtower.domain.admin.dto.MemberStatusUpdateRequestDto
import kr.byeongmin.watchtower.domain.admin.dto.TowerStatusUpdateRequestDto
import kr.byeongmin.watchtower.domain.admin.service.AdminService
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.domain.tower.dto.TowerResponseDto
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.OffsetPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.OffsetPaginationResponseDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.time.LocalDateTime

class AdminControllerTest {
    private val objectMapper = jacksonObjectMapper()

    private val adminService = mock<AdminService>()
    private val mockMvc = MockMvcBuilders.standaloneSetup(AdminController(adminService)).build()
    private val memberResponseDto = MemberResponseDto(
        id = 21L,
        email = "member@example.com",
        nickname = "회원",
        profileImageUrl = "https://example.com/profile",
        role = MemberRole.NORMAL,
        bannedAt = null,
        banReason = null,
        lastSignInAt = LocalDateTime.of(2026, 10, 20, 12, 0),
        deletedAt = null
    )

    @Test
    fun `정렬 기준을 생략해 타워 목록을 요청하면 ID 기준으로 조회함`() {
        /** given */
        val request = OffsetPaginationRequestDto(
            query = "재고",
            page = 0,
            size = 10,
            sortDirection = Sort.Direction.ASC
        )
        whenever(adminService.getTowers(request, BaseSortField.ID)).thenReturn(
            SuccessDataResponse(
                OffsetPaginationResponseDto(
                    data = emptyList(),
                    page = 0,
                    size = 10,
                    totalElements = 0,
                    totalPages = 0
                )
            )
        )

        /** when */
        val result = mockMvc.perform(
            get("/api/v1/admin/towers")
                .param("query", "재고")
                .param("page", "0")
                .param("size", "10")
                .param("sortDirection", Sort.Direction.ASC.name)
        )

        /** then */
        result
            .andExpect(status().isOk)

        verify(adminService).getTowers(request, BaseSortField.ID)
    }

    @ParameterizedTest
    @EnumSource(value = BaseSortField::class, names = ["CREATED_AT", "UPDATED_AT"])
    fun `관리자는 검색어와 정렬 기준을 지정해 타워 목록을 조회할 수 있음`(sortField: BaseSortField) {
        /** given */
        whenever(adminService.getTowers(any(), eq(sortField))).thenReturn(
            SuccessDataResponse(
                OffsetPaginationResponseDto(
                    data = listOf(
                        TowerResponseDto(
                            id = 7L,
                            name = "재고 감시",
                            url = "https://example.com/products",
                            fetchInterval = Duration.ofMinutes(10),
                            includeInStat = true,
                            status = TowerStatus.ILLEGAL_SUSPENDED,
                            faviconUrl = null,
                            lastFetchedAt = null,
                            lastNotifiedAt = null
                        )
                    ),
                    page = 2,
                    size = 10,
                    totalElements = 21,
                    totalPages = 3
                )
            )
        )

        /** when */
        val result = mockMvc.perform(
            get("/api/v1/admin/towers")
                .param("query", "재고")
                .param("page", "2")
                .param("size", "10")
                .param("sortField", sortField.name)
                .param("sortDirection", Sort.Direction.ASC.name)
        )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.data[0].id").value(7))

        verify(adminService).getTowers(
            OffsetPaginationRequestDto(query = "재고", page = 2, size = 10, sortDirection = Sort.Direction.ASC),
            sortField
        )
    }

    @ParameterizedTest
    @EnumSource(value = BaseSortField::class, names = ["CREATED_AT", "UPDATED_AT"])
    fun `관리자는 검색어와 정렬 기준을 지정해 회원 목록을 조회할 수 있음`(sortField: BaseSortField) {
        /** given */
        whenever(adminService.getMembers(any(), eq(sortField)))
            .thenReturn(
                SuccessDataResponse(
                    OffsetPaginationResponseDto(
                        data = listOf(memberResponseDto),
                        page = 2,
                        size = 10,
                        totalElements = 21,
                        totalPages = 3
                    )
                )
            )

        /** when */
        val result = mockMvc
            .perform(
                get("/api/v1/admin/members")
                    .param("query", "회원")
                    .param("page", "2")
                    .param("size", "10")
                    .param("sortField", sortField.name)
                    .param("sortDirection", Sort.Direction.ASC.name)
            )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.data[0].id").value(21))

        verify(adminService).getMembers(
            OffsetPaginationRequestDto(query = "회원", page = 2, size = 10, sortDirection = Sort.Direction.ASC),
            sortField
        )
    }

    @Test
    fun `관리자는 타워를 정책 위반으로 정지할 수 있음`() {
        /** given */
        whenever(adminService.updateTowerStatus(eq(7L), any()))
            .thenReturn(
                SuccessDataResponse(
                    TowerResponseDto(
                        id = 7L,
                        name = "타워",
                        url = "https://example.com",
                        fetchInterval = Duration.ofMinutes(10),
                        includeInStat = true,
                        status = TowerStatus.ILLEGAL_SUSPENDED,
                        faviconUrl = null,
                        lastFetchedAt = null,
                        lastNotifiedAt = null
                    )
                )
            )

        /** when */
        val result = mockMvc
            .perform(
                put("/api/v1/admin/towers/7")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsBytes(
                            TowerStatusUpdateRequestDto(status = TowerStatus.ILLEGAL_SUSPENDED)
                        )
                    )
            )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value(TowerStatus.ILLEGAL_SUSPENDED.name))

        verify(adminService).updateTowerStatus(
            towerId = 7L,
            towerStatusUpdateRequestDto = TowerStatusUpdateRequestDto(status = TowerStatus.ILLEGAL_SUSPENDED)
        )
    }

    @Test
    fun `관리자는 회원에게 관리자 권한을 부여할 수 있음`() {
        /** given */
        whenever(adminService.updateMemberRole(eq(21L), any()))
            .thenReturn(SuccessDataResponse(memberResponseDto.copy(role = MemberRole.ADMIN)))

        /** when */
        val result = mockMvc
            .perform(
                put("/api/v1/admin/members/21/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsBytes(
                            MemberRoleUpdateRequestDto(role = MemberRole.ADMIN)
                        )
                    )
            )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.role").value(MemberRole.ADMIN.name))

        verify(adminService).updateMemberRole(
            memberId = 21L,
            memberRoleUpdateRequestDto = MemberRoleUpdateRequestDto(role = MemberRole.ADMIN)
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `회원 정지와 해제 결과에는 현재 정지 시각과 사유가 반영됨`(banned: Boolean) {
        /** given */
        val bannedAt = if (banned)
            LocalDateTime.of(2026, 10, 20, 12, 34, 56)
        else
            null
        val banReason = if (banned) "반복적인 정책 위반" else null

        whenever(adminService.updateMemberStatus(eq(21L), any()))
            .thenReturn(SuccessDataResponse(memberResponseDto.copy(bannedAt = bannedAt, banReason = banReason)))

        /** when */
        val result = mockMvc
            .perform(
                put("/api/v1/admin/members/21/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsBytes(
                            MemberStatusUpdateRequestDto(banned = banned, banReason = banReason.orEmpty())
                        )
                    )
            )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.bannedAt").value(bannedAt?.toString()))
            .andExpect(jsonPath("$.data.banReason").value(banReason))

        verify(adminService).updateMemberStatus(
            memberId = 21L,
            memberStatusUpdateRequestDto = MemberStatusUpdateRequestDto(banned = banned, banReason = banReason.orEmpty())
        )
    }
}
