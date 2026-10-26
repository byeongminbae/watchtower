package kr.byeongmin.watchtower.domain.auth.service

import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SessionMemberLoaderImplTest {
    private val memberAccessor = mock<MemberAccessor>()
    private val sessionMemberLoader = SessionMemberLoaderImpl(memberAccessor)
    private val now = LocalDateTime.of(2026, 10, 26, 12, 0)
    private val member = Member.from(
        NaverProfileResponseDto(
            resultCode = "00",
            message = "성공",
            response = NaverProfileResponseDto.NaverProfileDetailDto(
                providerId = "naver-id",
                email = "member@example.com",
                nickname = "회원",
                profileImage = "https://example.com/profile.png"
            )
        ),
        now,
        mock()
    )

    @Test
    fun `세션의 회원을 찾을 수 없으면 계정 부재 오류로 처리함`() {
        /** given */
        whenever(memberAccessor.findMember(21L)).thenReturn(null)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            sessionMemberLoader.getMemberPrinciple(21L)
        }

        /** then */
        assertEquals(AuthError.ACCOUNT_NOT_FOUND, exception.error)
    }

    @Test
    fun `세션의 회원이 정지되면 정지된 계정 오류로 처리함`() {
        /** given */
        member.ban(now, "반복적인 정책 위반")
        whenever(memberAccessor.findMember(21L)).thenReturn(member)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            sessionMemberLoader.getMemberPrinciple(21L)
        }

        /** then */
        assertEquals(AuthError.BANNED_ACCOUNT, exception.error)
    }

    @Test
    fun `세션의 권한은 회원의 현재 권한을 사용함`() {
        /** given */
        member.changeRole(MemberRole.ADMIN)
        whenever(memberAccessor.findMember(21L)).thenReturn(member)

        /** when */
        val principle = sessionMemberLoader.getMemberPrinciple(21L)

        /** then */
        assertEquals(21L, principle.memberId)
        assertEquals(MemberRole.ADMIN.name, principle.role)
    }
}
