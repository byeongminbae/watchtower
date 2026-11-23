package kr.byeongmin.watchtower.domain.notification.notifier

import kr.byeongmin.watchtower.domain.auth.entity.DiscordAccount
import kr.byeongmin.watchtower.domain.auth.error.SocialAccountError
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.notification.error.NotificationError
import kr.byeongmin.watchtower.external.discord.client.DiscordClient
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class SocialAccountNotifierImplTest {
    private val discordClient = mock<DiscordClient>()
    private val socialAccountNotifier = SocialAccountNotifierImpl(discordClient)

    private val content = "알림 내용"
    private val directMessageChannelId = "1573265772178968700"
    private val discordAccount = DiscordAccount.of(mock<Member>(), "80351110224678912", directMessageChannelId)

    @Test
    fun `Discord 계정에는 DM 채널로 내용을 그대로 보냄`() {
        /** when */
        socialAccountNotifier.send(discordAccount, content)

        /** then */
        verify(discordClient).sendMessage(directMessageChannelId, content)
    }

    @Test
    fun `DM 을 막아 둔 Discord 계정에 보내면 받을 수 없는 계정 오류가 발생함`() {
        /** given */
        doThrow(BusinessException(SocialAccountError.DISCORD_DIRECT_MESSAGE_BLOCKED))
            .whenever(discordClient).sendMessage(directMessageChannelId, content)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            socialAccountNotifier.send(discordAccount, content)
        }

        /** then */
        assertEquals(NotificationError.RECIPIENT_UNREACHABLE, exception.error)
    }

    @Test
    fun `그 밖의 Discord 오류는 그대로 다시 던짐`() {
        /** given */
        val discordError = BusinessException(CommonError.EXTERNAL_API_ERROR)
        doThrow(discordError).whenever(discordClient).sendMessage(directMessageChannelId, content)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            socialAccountNotifier.send(discordAccount, content)
        }

        /** then */
        assertSame(discordError, exception)
    }
}
