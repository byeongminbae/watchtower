package kr.byeongmin.watchtower.domain.member.entity

import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.domain.subscription.entity.Plan
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MemberTest {
    private val plan = mock<Plan>()
    private val now = LocalDateTime.of(2026, 10, 20, 12, 0)

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

    @Test
    fun `네이버로 가입한 회원은 네이버에서 내려온 정보를 사용하고 일반 회원 권한을 가짐`() {
        /** when */
        val member = Member.from(naverProfile, now, plan)

        /** then */
        assertEquals(naverProfile.response.email, member.email)
        assertEquals(naverProfile.response.nickname, member.nickname)
        assertEquals(naverProfile.response.profileImage, member.profileImageUrl)
        assertEquals(MemberRole.NORMAL, member.role)
        assertNull(member.refreshToken)
        assertEquals(now, member.lastSignInAt)
    }

    @Test
    fun `로그인하면 새 리프레시 토큰과 로그인한 시각이 기록됨`() {
        /** given */
        val previousSignInAt = LocalDateTime.of(2000, 1, 1, 0, 0)
        val member = Member.from(naverProfile, previousSignInAt, plan)
        member.completeSignIn("기존-토큰", previousSignInAt)

        /** when */
        member.completeSignIn("new-refresh", now)

        /** then */
        assertEquals("new-refresh", member.refreshToken)
        assertEquals(now, member.lastSignInAt)
    }

    @Test
    fun `토큰을 다시 발급받아도 최근 로그인 시각이 갱신됨`() {
        /** given */
        val member = Member.from(naverProfile, now.minusDays(30), plan)
        val rotatedAt = now

        /** when */
        member.rotateRefreshToken("refresh", rotatedAt)

        /** then */
        assertEquals("refresh", member.refreshToken)
        assertEquals(rotatedAt, member.lastSignInAt)
    }

    @Test
    fun `로그아웃하면 기존 리프레시 토큰이 제거됨`() {
        /** given */
        val member = Member.from(naverProfile, now, plan)
        member.rotateRefreshToken("refresh", now)

        /** when */
        member.signOut()

        /** then */
        assertNull(member.refreshToken)
    }

    @ParameterizedTest
    @CsvSource("4,true", "5,false", "6,false")
    fun `이용 중인 플랜의 한도에 도달하면 타워를 더 등록할 수 없음`(count: Long, expected: Boolean) {
        /** given */
        whenever(plan.maxTowerQuantity).thenReturn(5)
        val member = Member.from(naverProfile, now, plan)

        /** when */
        val result = member.isTowerAddable(count)

        /** then */
        assertEquals(expected, result)
    }
}
