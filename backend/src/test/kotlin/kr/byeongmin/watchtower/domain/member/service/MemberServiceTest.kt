package kr.byeongmin.watchtower.domain.member.service

import kr.byeongmin.watchtower.domain.auth.accessor.NaverOAuthAccessor
import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.dto.MemberUpdateRequestDto
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.notification.accessor.NotificationChannelAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.dto.TowerCreateRequestDto
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.external.naver.dto.NaverTokenResponseDto
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MemberServiceTest {
    private val memberAccessor = mock<MemberAccessor>()
    private val notificationChannelAccessor = mock<NotificationChannelAccessor>()
    private val towerAccessor = mock<TowerAccessor>()
    private val naverOAuthAccessor = mock<NaverOAuthAccessor>()
    private val now = LocalDateTime.of(2026, 10, 20, 12, 0)
    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn now
    }

    private val memberService = MemberService(
        memberAccessor = memberAccessor,
        notificationChannelAccessor = notificationChannelAccessor,
        towerAccessor = towerAccessor,
        naverOAuthAccessor = naverOAuthAccessor,
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

    private val member = spy(Member.from(naverProfile, now, mock())) {
        on { id } doReturn 21L
    }
    private val request = TowerCreateRequestDto(
        name = "타워",
        url = TowerUrl.from("https://example.com"),
        fetchInterval = Duration.ofMinutes(10),
        includeInStat = true
    )
    private val tower = spy(Tower.from(member, request)) {
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
        assertEquals(member.email, response.data.email)
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
    fun `타워 목록에는 삭제되지 않은 마지막 변경의 AI 요약이 표시됨`() {
        /** given */
        val memberId = 21L
        val towerWithoutSnapshot = spy(Tower.from(member, request)) {
            on { id } doReturn 6L
        }
        val pagination = CursorPaginationRequestDto(size = 20)
        tower.snapshots.addAll(
            listOf(
                mock {
                    on { id } doReturn 14L
                    on { createdAt } doReturn now.minusMonths(2)
                    on { aiDiffSummary } doReturn "보관 기간이 지난 변경 내용"
                },
                mock {
                    on { id } doReturn 10L
                    on { createdAt } doReturn now.minusMonths(3)
                    on { aiDiffSummary } doReturn "이전 변경 내용"
                },
                mock {
                    on { id } doReturn 15L
                    on { isDeleted() } doReturn true
                    on { aiDiffSummary } doReturn "삭제된 변경 요약"
                }
            )
        )
        whenever(towerAccessor.findOwnedTowers(memberId, pagination, BaseSortField.CREATED_AT))
            .thenReturn(listOf(tower, towerWithoutSnapshot))

        /** when */
        val response = memberService.getMemberTowers(memberId, pagination, BaseSortField.CREATED_AT)

        /** then */
        assertEquals(2, response.data.data.size)
        assertEquals(7L, response.data.data[0].id)
        assertEquals(tower.name, response.data.data[0].name)
        assertEquals(tower.status, response.data.data[0].status)
        assertEquals("보관 기간이 지난 변경 내용", response.data.data[0].latestAiSummary)
        assertEquals(6L, response.data.data[1].id)
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
    fun `네이버 연동 정보가 없어도 회원은 탈퇴할 수 있음`() {
        /** given */
        val memberId = 21L
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)
        whenever(naverOAuthAccessor.findByMemberId(memberId)).thenReturn(null)

        /** when */
        memberService.deleteMember(memberId)

        /** then */
        assertNotNull(member.deletedAt)
    }

    @Test
    fun `회원이 탈퇴하면 네이버 연동 정보도 삭제 처리됨`() {
        /** given */
        val memberId = 21L
        val oauth = NaverOAuth.from(
            member,
            naverProfile,
            NaverTokenResponseDto(
                accessToken = "access",
                refreshToken = "refresh",
                tokenType = "bearer",
                expiresIn = 3600L
            )
        )
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)
        whenever(naverOAuthAccessor.findByMemberId(memberId)).thenReturn(oauth)

        /** when */
        memberService.deleteMember(memberId)

        /** then */
        assertNotNull(member.deletedAt)
        assertNotNull(oauth.deletedAt)
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
        inOrder(towerAccessor, notificationChannelAccessor, member) {
            verify(towerAccessor).deleteAllRelatedByMemberId(memberId, now)
            verify(notificationChannelAccessor).deleteByMemberId(memberId, now)
            verify(member).delete()
        }
    }

    @Test
    fun `타워 관련 엔티티를 정리하지 못하면 회원을 탈퇴 처리하지 않음`() {
        /** given */
        val memberId = 21L
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)
        whenever(towerAccessor.deleteAllRelatedByMemberId(memberId, now))
            .thenThrow(IllegalStateException("snapshot update failed"))

        /** when */
        assertFailsWith<IllegalStateException> {
            memberService.deleteMember(memberId)
        }

        /** then */
        assertNull(member.deletedAt)
        verifyNoInteractions(notificationChannelAccessor)
    }

    @Test
    fun `알림 채널을 정리하지 못하면 회원을 탈퇴 처리하지 않음`() {
        /** given */
        val memberId = 21L
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)
        whenever(notificationChannelAccessor.deleteByMemberId(memberId, now))
            .thenThrow(IllegalStateException("channel update failed"))

        /** when */
        assertFailsWith<IllegalStateException> {
            memberService.deleteMember(memberId)
        }

        /** then */
        assertNull(member.deletedAt)
        verify(towerAccessor).deleteAllRelatedByMemberId(memberId, now)
    }

    @Test
    fun `최초 수집본만 있으면 타워 목록의 최근 변경 요약이 없음`() {
        /** given */
        tower.snapshots.add(
            mock {
                on { id } doReturn 10L
                on { isHead() } doReturn true
            }
        )
        val request = CursorPaginationRequestDto(size = 10)
        whenever(towerAccessor.findOwnedTowers(21L, request, BaseSortField.ID)).thenReturn(listOf(tower))

        /** when */
        val response = memberService.getMemberTowers(21L, request, BaseSortField.ID)

        /** then */
        assertNull(response.data.data.single().latestAiSummary)
    }

    @Test
    fun `마지막 변경에 AI 요약이 없으면 이전 변경의 요약을 대신 보여주지 않음`() {
        /** given */
        tower.snapshots.addAll(
            listOf(
                mock {
                    on { id } doReturn 10L
                    on { aiDiffSummary } doReturn "이전 요약"
                },
                mock { on { id } doReturn 11L }
            )
        )
        val request = CursorPaginationRequestDto(size = 10)
        whenever(towerAccessor.findOwnedTowers(21L, request, BaseSortField.ID)).thenReturn(listOf(tower))

        /** when */
        val response = memberService.getMemberTowers(21L, request, BaseSortField.ID)

        /** then */
        assertNull(response.data.data.single().latestAiSummary)
    }
}
