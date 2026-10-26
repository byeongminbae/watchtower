package kr.byeongmin.watchtower.domain.member.entity

import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.domain.subscription.entity.Plan
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
        /** given */
        val signUpAt = LocalDateTime.of(2026, 10, 20, 12, 0)

        /** when */
        val member = Member.from(naverProfile, signUpAt, plan)

        /** then */
        assertEquals(naverProfile.response.email, member.email)
        assertEquals(naverProfile.response.nickname, member.nickname)
        assertEquals(naverProfile.response.profileImage, member.profileImageUrl)
        assertEquals(MemberRole.NORMAL, member.role)
        assertEquals(signUpAt, member.lastSignInAt)
    }

    @Test
    fun `로그인하면 마지막 로그인 시각이 갱신됨`() {
        /** given */
        val previousSignInAt = LocalDateTime.of(2000, 1, 1, 0, 0)
        val member = Member.from(naverProfile, previousSignInAt, plan)

        /** when */
        member.ensureSignInAllowed()
        member.completeSignIn(now)

        /** then */
        assertEquals(now, member.lastSignInAt)
    }

    @Test
    fun `정지된 회원은 로그인할 수 없음`() {
        /** given */
        val member = Member.from(naverProfile, now, plan)
        member.ban(now, "반복적인 정책 위반")

        /** when */
        val exception = assertFailsWith<BusinessException> {
            member.ensureSignInAllowed()
        }

        /** then */
        assertEquals(AuthError.BANNED_ACCOUNT, exception.error)
    }

    @Test
    fun `탈퇴한 회원은 로그인할 수 없음`() {
        /** given */
        val member = Member.from(naverProfile, now, plan)
        member.delete()

        /** when */
        val exception = assertFailsWith<BusinessException> {
            member.ensureSignInAllowed()
        }

        /** then */
        assertEquals(AuthError.ALREADY_DELETED_ACCOUNT, exception.error)
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
