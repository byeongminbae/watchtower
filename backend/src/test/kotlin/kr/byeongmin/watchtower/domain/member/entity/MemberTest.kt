package kr.byeongmin.watchtower.domain.member.entity

import kr.byeongmin.watchtower.domain.auth.entity.DiscordAccount
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.domain.subscription.entity.Plan
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
import kotlin.test.assertNotNull

class MemberTest {
    private val plan = mock<Plan>()
    private val now = LocalDateTime.of(2026, 10, 20, 12, 0)

    @Test
    fun `새로 가입한 회원은 받은 정보로 만들어지고 일반 회원 권한을 가짐`() {
        /** given */
        val signUpAt = LocalDateTime.of(2026, 10, 20, 12, 0)

        /** when */
        val member = Member.of(
            nickname = "회원",
            profileImageUrl = "https://example.com/profile.png",
            lastSignInAt = signUpAt,
            plan = plan
        )

        /** then */
        assertEquals("회원", member.nickname)
        assertEquals("https://example.com/profile.png", member.profileImageUrl)
        assertEquals(MemberRole.NORMAL, member.role)
        assertEquals(signUpAt, member.lastSignInAt)
    }

    @Test
    fun `정지된 회원은 로그인할 수 없음`() {
        /** given */
        val member = Member.of(
            nickname = "회원",
            profileImageUrl = "https://example.com/profile.png",
            lastSignInAt = now,
            plan = plan
        )
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
        val member = Member.of(
            nickname = "회원",
            profileImageUrl = "https://example.com/profile.png",
            lastSignInAt = now,
            plan = plan
        )
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
        val member = Member.of(
            nickname = "회원",
            profileImageUrl = "https://example.com/profile.png",
            lastSignInAt = now,
            plan = plan
        )

        /** when */
        val result = member.isTowerAddable(count)

        /** then */
        assertEquals(expected, result)
    }

    @Test
    fun `회원을 삭제하면 연결된 계정도 함께 삭제됨`() {
        /** given */
        val member = Member.of(
            nickname = "회원",
            profileImageUrl = "https://example.com/profile.png",
            lastSignInAt = now,
            plan = plan
        )
        val discordAccount = DiscordAccount.of(
            member = member,
            providerId = "80351110224678912",
            directMessageChannelId = "1573265772178968700"
        )

        /** when */
        member.delete()

        /** then */
        assertNotNull(member.deletedAt)
        assertNotNull(discordAccount.deletedAt)
    }
}
