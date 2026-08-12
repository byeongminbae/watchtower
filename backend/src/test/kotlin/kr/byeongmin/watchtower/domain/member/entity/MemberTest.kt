package kr.byeongmin.watchtower.domain.member.entity

import kr.byeongmin.watchtower.domain.auth.dto.NaverProfileResponseExternalDto
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemberTest {
    private val naverProfile = NaverProfileResponseExternalDto(
        resultCode = "00",
        message = "성공",
        response = NaverProfileResponseExternalDto.NaverProfileDetailDto(
            providerId = "네이버-고객-고유-아이디",
            email = "네이버-고객-이메일",
            nickname = "네이버-고객-닉네임",
            profileImage = "https://naver.com/user/123/profile.png",
        ),
    )

    @Test
    fun `네이버 프로필이 주어진 상황에서 회원을 생성하면 프로필과 일반 회원 역할을 기본값으로 설정한다`() {
        // When
        val member = Member.from(naverProfile)

        // Then
        assertEquals(naverProfile.response.email, member.email)
        assertEquals(naverProfile.response.nickname, member.nickname)
        assertEquals(naverProfile.response.profileImage, member.profileImageUrl)
        assertEquals(MemberRole.NORMAL, member.role)
        assertNull(member.refreshToken)
        assertNotNull(member.lastSignInAt)
    }

    @Test
    fun `기존 회원인 상황에서 로그인을 완료하면 리프레시 토큰과 최근 로그인 시각을 갱신한다`() {
        // Given
        val member = Member.from(naverProfile)
        val previousSignInAt = LocalDateTime.of(2000, 1, 1, 0, 0)
        Member::class.java.getDeclaredField("lastSignInAt").apply {
            isAccessible = true
            set(member, previousSignInAt)
        }

        // When
        member.completeSignIn("새로-발급한-와치타워-리프레시-토큰")

        // Then
        assertEquals("새로-발급한-와치타워-리프레시-토큰", member.refreshToken)
        assertTrue(member.lastSignInAt.isAfter(previousSignInAt))
    }

    @Test
    fun `기존 회원인 상황에서 리프레시 토큰을 교체하면 최근 로그인 시각을 유지한다`() {
        // Given
        val member = Member.from(naverProfile)
        val originalSignInAt = member.lastSignInAt

        // When
        member.rotateRefreshToken("와치타워-리프레시-토큰")

        // Then
        assertEquals("와치타워-리프레시-토큰", member.refreshToken)
        assertEquals(originalSignInAt, member.lastSignInAt)
    }

    @Test
    fun `리프레시 토큰이 있는 회원인 상황에서 로그아웃하면 리프레시 토큰을 삭제한다`() {
        // Given
        val member = Member.from(naverProfile)
        member.rotateRefreshToken("와치타워-리프레시-토큰")

        // When
        member.signOut()

        // Then
        assertNull(member.refreshToken)
    }
}
