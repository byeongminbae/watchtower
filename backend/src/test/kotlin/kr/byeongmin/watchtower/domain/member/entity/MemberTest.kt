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
    fun `네이버 프로필로 일반 회원 생성`() {
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
    fun `로그인한 경우`() {
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
    fun `토큰 로테이션은 최근 로그인 시각과 무관`() {
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
    fun `로그아웃한 경우`() {
        // Given
        val member = Member.from(naverProfile)
        member.rotateRefreshToken("와치타워-리프레시-토큰")

        // When
        member.signOut()

        // Then
        assertNull(member.refreshToken)
    }
}
