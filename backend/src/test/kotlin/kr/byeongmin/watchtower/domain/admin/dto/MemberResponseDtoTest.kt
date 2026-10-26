package kr.byeongmin.watchtower.domain.admin.dto

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.LocalDateTime
import kotlin.test.assertEquals

class MemberResponseDtoTest {
    @Test
    fun `관리자용 회원 응답에는 정지 시각과 사유 및 탈퇴 시각도 담김`() {
        /** given */
        val bannedAt = LocalDateTime.of(2026, 10, 1, 12, 0)
        val banReason = "반복적인 정책 위반"
        val deletedAt = bannedAt.plusDays(1)
        val lastSignInAt = bannedAt.minusDays(1)
        val member = mock<Member> {
            on { id } doReturn 21L
            on { email } doReturn "member@example.com"
            on { nickname } doReturn "회원"
            on { profileImageUrl } doReturn "https://example.com/profile.png"
            on { role } doReturn MemberRole.NORMAL
            on { this.bannedAt } doReturn bannedAt
            on { this.banReason } doReturn banReason
            on { this.deletedAt } doReturn deletedAt
            on { this.lastSignInAt } doReturn lastSignInAt
        }

        /** when */
        val response = MemberResponseDto.from(member)

        /** then */
        assertEquals(bannedAt, response.bannedAt)
        assertEquals(banReason, response.banReason)
        assertEquals(deletedAt, response.deletedAt)
        assertEquals(lastSignInAt, response.lastSignInAt)
    }
}
