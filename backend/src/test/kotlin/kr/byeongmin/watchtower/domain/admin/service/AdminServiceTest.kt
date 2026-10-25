package kr.byeongmin.watchtower.domain.admin.service

import kr.byeongmin.watchtower.domain.admin.dto.MemberRoleUpdateRequestDto
import kr.byeongmin.watchtower.domain.admin.dto.MemberStatusUpdateRequestDto
import kr.byeongmin.watchtower.domain.admin.dto.TowerStatusUpdateRequestDto
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.dto.TowerCreateRequestDto
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.OffsetPaginationRequestDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminServiceTest {
    private val towerAccessor = mock<TowerAccessor>()
    private val memberAccessor = mock<MemberAccessor>()
    private val timeProvider = mock<TimeProvider>()

    private val adminService = AdminService(
        towerAccessor = towerAccessor,
        memberAccessor = memberAccessor,
        timeProvider = timeProvider
    )

    private val naverProfile = NaverProfileResponseDto(
        resultCode = "00",
        message = "성공",
        response = NaverProfileResponseDto.NaverProfileDetailDto(
            providerId = "naver-id",
            email = "member@example.com",
            nickname = "회원",
            profileImage = "https://example.com/profile.png"
        )
    )

    private val memberId = 21L
    private val towerId = 7L
    private val now = LocalDateTime.of(2026, 10, 2, 12, 0)

    private val member = spy(Member.from(naverProfile, now, mock()).apply {
        changeRole(MemberRole.NORMAL)
        unban()
    }) {
        on { id } doReturn memberId
    }
    private val tower = spy(
        Tower.from(
            member,
            TowerCreateRequestDto(
                name = "타워",
                url = TowerUrl.from("https://example.com"),
                fetchInterval = Duration.ofMinutes(10),
                includeInStat = true
            )
        )
    ) {
        on { id } doReturn towerId
    }

    @Test
    fun `관리자는 정책을 위반한 타워를 정지할 수 있음`() {
        /** given */
        whenever(towerAccessor.getTower(towerId)).thenReturn(tower)

        /** when */
        val response = adminService.updateTowerStatus(
            towerId,
            towerStatusUpdateRequestDto = TowerStatusUpdateRequestDto(TowerStatus.ILLEGAL_SUSPENDED)
        )

        /** then */
        assertEquals(TowerStatus.ILLEGAL_SUSPENDED, response.data.status)
        assertEquals(TowerStatus.ILLEGAL_SUSPENDED, tower.status)
    }

    @Test
    fun `관리자도 존재하지 않는 타워의 상태는 변경할 수 없음`() {
        /** given */
        whenever(towerAccessor.getTower(towerId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            adminService.updateTowerStatus(
                towerId,
                towerStatusUpdateRequestDto = TowerStatusUpdateRequestDto(TowerStatus.ILLEGAL_SUSPENDED)
            )
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `관리자는 타워 검색 결과에서 이름과 URL를 확인할 수 있음`() {
        /** given */
        val request = OffsetPaginationRequestDto(
            query = "타워",
            page = 2,
            size = 10,
            sortDirection = Sort.Direction.DESC
        )
        val pageable = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        whenever(towerAccessor.findTowers(request, BaseSortField.CREATED_AT))
            .thenReturn(PageImpl(listOf(tower), pageable, 21L))

        /** when */
        val response = adminService.getTowers(
            request,
            BaseSortField.CREATED_AT
        )

        /** then */
        assertEquals(1, response.data.data.size)
        assertEquals(tower.id, response.data.data.first().id)
        assertEquals(tower.name, response.data.data.first().name)
        assertEquals(tower.url, response.data.data.first().url)
    }

    @Test
    fun `검색 조건에 맞는 타워가 없으면 검색 결과는 비어 있음`() {
        /** given */
        val request = OffsetPaginationRequestDto(
            query = "없는이름",
            page = 0,
            size = 20,
            sortDirection = Sort.Direction.DESC
        )
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"))
        whenever(towerAccessor.findTowers(request, BaseSortField.ID))
            .thenReturn(PageImpl(emptyList(), pageable, 0L))

        /** when */
        val response = adminService.getTowers(
            request,
            BaseSortField.ID
        )

        /** then */
        assertTrue(response.data.data.isEmpty())
    }

    @Test
    fun `관리자는 회원 목록에서 각 회원의 권한과 정지 여부를 확인할 수 있음`() {
        /** given */
        val request = OffsetPaginationRequestDto(
            query = "",
            page = 0,
            size = 20,
            sortDirection = Sort.Direction.DESC
        )
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"))
        whenever(memberAccessor.findMembers(request, BaseSortField.UPDATED_AT))
            .thenReturn(PageImpl(listOf(member), pageable, 1L))

        /** when */
        val response = adminService.getMembers(
            request,
            BaseSortField.UPDATED_AT
        )

        /** then */
        assertEquals(1, response.data.data.size)
        assertEquals(memberId, response.data.data.first().id)
        assertEquals(MemberRole.NORMAL, response.data.data.first().role)
        assertNull(response.data.data.first().bannedAt)
    }

    @Test
    fun `관리자는 일반 회원에게 관리자 권한을 부여할 수 있음`() {
        /** given */
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)

        /** when */
        val response = adminService.updateMemberRole(memberId, MemberRoleUpdateRequestDto(MemberRole.ADMIN))

        /** then */
        assertEquals(MemberRole.ADMIN, response.data.role)
        assertEquals(MemberRole.ADMIN, member.role)
    }

    @Test
    fun `관리자도 존재하지 않는 회원의 권한은 변경할 수 없음`() {
        /** given */
        whenever(memberAccessor.getMember(memberId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            adminService.updateMemberRole(memberId, MemberRoleUpdateRequestDto(MemberRole.ADMIN))
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `관리자가 회원을 정지하면 정지한 시각이 기록됨`() {
        /** given */
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)
        whenever(timeProvider.now()).thenReturn(now)

        /** when */
        val response = adminService.updateMemberStatus(
            memberId,
            MemberStatusUpdateRequestDto(banned = true)
        )

        /** then */
        assertEquals(now, response.data.bannedAt)
        assertEquals(now, member.bannedAt)
    }

    @Test
    fun `관리자가 회원의 정지를 해제하면 정지 시각도 지워짐`() {
        /** given */
        member.ban(now.minusDays(1))
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)
        whenever(timeProvider.now()).thenReturn(now)

        /** when */
        val response = adminService.updateMemberStatus(
            memberId,
            MemberStatusUpdateRequestDto(banned = false)
        )

        /** then */
        assertNull(response.data.bannedAt)
        assertNull(member.bannedAt)
    }

    @Test
    fun `관리자도 존재하지 않는 회원을 정지할 수 없음`() {
        /** given */
        whenever(memberAccessor.getMember(memberId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            adminService.updateMemberStatus(
                memberId,
                MemberStatusUpdateRequestDto(banned = true)
            )
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }
}
