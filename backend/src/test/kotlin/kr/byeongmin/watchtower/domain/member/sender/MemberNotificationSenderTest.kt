package kr.byeongmin.watchtower.domain.member.sender

import kr.byeongmin.watchtower.domain.auth.accessor.DiscordAccountAccessor
import kr.byeongmin.watchtower.domain.auth.entity.DiscordAccount
import kr.byeongmin.watchtower.domain.auth.error.SocialAccountError
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.external.discord.client.DiscordClient
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MemberNotificationSenderTest {
    private val discordAccountAccessor = mock<DiscordAccountAccessor>()
    private val discordClient = mock<DiscordClient>()
    private val memberNotificationSender = MemberNotificationSender(
        discordAccountAccessor = discordAccountAccessor,
        discordClient = discordClient
    )

    private val memberId = 21L
    private val member = mock<Member> {
        on { id } doReturn memberId
        on { nickname } doReturn "넬리"
    }

    @Test
    fun `테스트 알림은 회원에 연결된 Discord 계정의 DM 으로 닉네임을 담아 바로 보냄`() {
        /** given */
        val directMessageChannelId = "1573265772178968700"
        val discordAccount = DiscordAccount.of(member, "80351110224678912", directMessageChannelId)
        whenever(discordAccountAccessor.getDiscordAccountOfMember(memberId)).thenReturn(discordAccount)

        /** when */
        memberNotificationSender.sendTestNotification(member)

        /** then */
        verify(discordClient).sendMessage(
            directMessageChannelId,
            "### 넬리 님! 테스트 알림이 잘 도착했어요\n지켜보는 페이지에 변화가 생기면 이렇게 알려드릴게요."
        )
    }

    @Test
    fun `알림을 받을 계정이 연결되어 있지 않으면 테스트 알림을 보낼 수 없음`() {
        /** given */
        whenever(discordAccountAccessor.getDiscordAccountOfMember(memberId))
            .thenThrow(BusinessException(SocialAccountError.SOCIAL_ACCOUNT_NOT_LINKED))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            memberNotificationSender.sendTestNotification(member)
        }

        /** then */
        assertEquals(SocialAccountError.SOCIAL_ACCOUNT_NOT_LINKED, exception.error)
        verifyNoInteractions(discordClient)
    }
}
