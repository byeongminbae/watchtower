package kr.byeongmin.watchtower.domain.member.service

import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.dto.MemberUpdateRequestDto
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.sender.MemberNotificationSender
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.dto.TowerCreateRequestDto
import kr.byeongmin.watchtower.domain.tower.vo.LatestChangeAiSummary
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.domain.Sort
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MemberServiceTest {
    private val memberAccessor = mock<MemberAccessor>()
    private val towerAccessor = mock<TowerAccessor>()
    private val towerSnapshotAccessor = mock<TowerSnapshotAccessor>()
    private val memberNotificationSender = mock<MemberNotificationSender>()
    private val now = LocalDateTime.of(2026, 10, 20, 12, 0)
    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn now
    }

    private val memberService = MemberService(
        memberAccessor = memberAccessor,
        towerAccessor = towerAccessor,
        towerSnapshotAccessor = towerSnapshotAccessor,
        memberNotificationSender = memberNotificationSender,
        timeProvider = timeProvider
    )

    private val member = spy(Member.of(
        nickname = "회원",
        profileImageUrl = "https://example.com/profile.png",
        lastSignInAt = now,
        plan = mock()
    )) {
        on { id } doReturn 21L
    }
    private val request = TowerCreateRequestDto(
        name = "타워",
        url = TowerUrl.from("https://example.com"),
        fetchInterval = Duration.ofMinutes(10),
        includeInStat = true
    )
    private val tower = spy(request.to(member)) {
        on { id } doReturn 7L
    }

    @Test
    fun `회원은 자신의 프로필을 조회할 수 있음`() {
        /** given */
        val memberId = 21L
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)

        /** when */
        val response = memberService.getMember(memberId)

        /** then */
        assertEquals(memberId, response.data.id)
        assertEquals(member.nickname, response.data.nickname)
    }

    @Test
    fun `존재하지 않는 회원의 프로필은 조회할 수 없음`() {
        /** given */
        val memberId = 99L
        whenever(memberAccessor.getMember(memberId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            memberService.getMember(memberId)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `타워 목록에는 타워마다 마지막 변경의 AI 요약이 표시되고 변경이 없던 타워는 요약이 없음`() {
        /** given */
        val memberId = 21L
        val towerWithoutChange = spy(request.to(member)) {
            on { id } doReturn 6L
        }
        val pagination = CursorPaginationRequestDto(size = 20)
        whenever(towerAccessor.findOwnedTowers(memberId, pagination, BaseSortField.CREATED_AT))
            .thenReturn(listOf(tower, towerWithoutChange))
        whenever(towerSnapshotAccessor.findLatestChangeAiSummaries(listOf(7L, 6L)))
            .thenReturn(listOf(LatestChangeAiSummary(towerId = 7L, aiSummary = "마지막 변경 내용")))

        /** when */
        val response = memberService.getMemberTowers(memberId, pagination, BaseSortField.CREATED_AT)

        /** then */
        assertEquals(listOf(7L, 6L), response.data.data.map { it.id })
        assertEquals(tower.name, response.data.data[0].name)
        assertEquals(tower.status, response.data.data[0].status)
        assertEquals("마지막 변경 내용", response.data.data[0].latestAiSummary)
        assertNull(response.data.data[1].latestAiSummary)
    }

    @Test
    fun `요청한 회원의 타워 목록을 검색 조건으로 조회함`() {
        /** given */
        val memberId = 21L
        doReturn(3L).whenever(tower).id
        val pagination = CursorPaginationRequestDto(
            query = "타워",
            cursorId = 5L,
            cursorValue = now.toString(),
            size = 10,
            sortDirection = Sort.Direction.DESC
        )
        whenever(towerAccessor.findOwnedTowers(memberId, pagination, BaseSortField.CREATED_AT))
            .thenReturn(listOf(tower))
        whenever(towerSnapshotAccessor.findLatestChangeAiSummaries(listOf(3L)))
            .thenReturn(emptyList())

        /** when */
        val response = memberService.getMemberTowers(memberId, pagination, BaseSortField.CREATED_AT)

        /** then */
        assertEquals(listOf(3L), response.data.data.map { it.id })
    }

    @Test
    fun `회원은 자신의 닉네임과 프로필 사진을 변경할 수 있음`() {
        /** given */
        val memberId = 21L
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)

        /** when */
        val response = memberService.updateMember(
            memberId,
            MemberUpdateRequestDto(
                nickname = "바뀐-닉네임",
                profileImageUrl = "https://example.com/changed-profile.png"
            )
        )

        /** then */
        assertEquals("바뀐-닉네임", response.data.nickname)
        assertEquals("https://example.com/changed-profile.png", member.profileImageUrl)
    }

    @Test
    fun `존재하지 않는 회원의 프로필은 변경할 수 없음`() {
        /** given */
        val memberId = 99L
        whenever(memberAccessor.getMember(memberId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            memberService.updateMember(
                memberId,
                MemberUpdateRequestDto(
                    nickname = "바뀐-닉네임",
                    profileImageUrl = "https://example.com/changed-profile.png"
                )
            )
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `존재하지 않는 회원은 탈퇴할 수 없음`() {
        /** given */
        val memberId = 21L
        whenever(memberAccessor.getMember(memberId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            memberService.deleteMember(memberId)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `회원 탈퇴 시 소유 리소스 정리를 요청한 뒤 탈퇴 처리함`() {
        /** given */
        val memberId = 21L
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)

        /** when */
        memberService.deleteMember(memberId)

        /** then */
        inOrder(towerAccessor, member) {
            verify(towerAccessor).deleteAllOwnedTowers(memberId, now)
            verify(member).delete()
        }
    }

    @Test
    fun `타워 관련 엔티티를 정리하지 못하면 회원을 탈퇴 처리하지 않음`() {
        /** given */
        val memberId = 21L
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)
        whenever(towerAccessor.deleteAllOwnedTowers(memberId, now))
            .thenThrow(IllegalStateException("snapshot update failed"))

        /** when */
        assertFailsWith<IllegalStateException> {
            memberService.deleteMember(memberId)
        }

        /** then */
        assertNull(member.deletedAt)
    }

    @Test
    fun `회원은 자신에게 연결된 계정으로 테스트 알림을 보낼 수 있음`() {
        /** given */
        whenever(memberAccessor.getMember(21L)).thenReturn(member)

        /** when */
        memberService.sendTestNotification(21L)

        /** then */
        verify(memberNotificationSender).sendTestNotification(member)
    }
}
