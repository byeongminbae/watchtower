package kr.byeongmin.watchtower.domain.auth.entity

import kr.byeongmin.watchtower.domain.auth.notifier.SocialAccountNotifier
import kr.byeongmin.watchtower.domain.member.entity.Member
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class DiscordAccountTest {
    @Test
    fun `소셜 계정으로 다뤄도 Discord 계정에 보내는 알림은 Discord 계정용 발송으로 보냄`() {
        /** given */
        val discordAccount = DiscordAccount.of(mock<Member>(), "80351110224678912", "1573265772178968700")
        val socialAccount: SocialAccount = discordAccount
        val socialAccountNotifier = mock<SocialAccountNotifier>()

        /** when */
        socialAccount.sendNotification(socialAccountNotifier, "알림 내용")

        /** then */
        verify(socialAccountNotifier).send(discordAccount, "알림 내용")
    }
}
