package kr.byeongmin.watchtower.domain.member.service

import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.domain.member.repository.MemberRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.support.MemberSupport
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MemberServiceTest {
    private val memberSupport = MemberSupport()
    private val memberRepository = mock<MemberRepository>()
    private val memberService = MemberService(memberRepository)

    private val naverProfile = NaverProfileResponseDto(
        resultCode = "00",
        message = "성공",
        response = NaverProfileResponseDto.NaverProfileDetailDto(
            providerId = "네이버-고객-고유-아이디",
            email = "네이버-고객-이메일",
            nickname = "네이버-고객-닉네임",
            profileImage = "https://naver.com/user/123/profile.png",
        ),
    )

    @Test
    fun `회원이 존재하는 상황에서 프로필을 조회하면 회원 정보를 반환한다`() {
        // Given
        val memberId = 21L
        val member = memberSupport.createMember(naverProfile, id = memberId)
        whenever(memberRepository.existsById(memberId)).thenReturn(true)
        whenever(memberRepository.findById(memberId)).thenReturn(Optional.of(member))

        // When
        val response = memberService.getMember(memberId)

        // Then
        assertEquals(memberId, response.data.id)
        assertEquals(member.email, response.data.email)
        verify(memberRepository).existsById(memberId)
        verify(memberRepository).findById(memberId)
    }

    @Test
    fun `회원이 존재하지 않는 상황에서 프로필을 조회하면 리소스 없음 예외를 던진다`() {
        // Given
        val memberId = 99L
        whenever(memberRepository.existsById(memberId)).thenReturn(false)

        // When
        val exception = assertFailsWith<BusinessException> {
            memberService.getMember(memberId)
        }

        // Then
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verify(memberRepository).existsById(memberId)
    }
}
