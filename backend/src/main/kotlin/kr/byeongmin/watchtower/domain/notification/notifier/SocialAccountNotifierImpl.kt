package kr.byeongmin.watchtower.domain.notification.notifier

import kr.byeongmin.watchtower.domain.auth.entity.DiscordAccount
import kr.byeongmin.watchtower.domain.auth.error.SocialAccountError
import kr.byeongmin.watchtower.domain.auth.notifier.SocialAccountNotifier
import kr.byeongmin.watchtower.domain.notification.error.NotificationError
import kr.byeongmin.watchtower.external.discord.client.DiscordClient
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class SocialAccountNotifierImpl(
    private val discordClient: DiscordClient
) : SocialAccountNotifier {
    override fun send(discordAccount: DiscordAccount, content: String) {
        try {
            discordClient.sendMessage(discordAccount.directMessageChannelId, content)
        } catch (exception: BusinessException) {
            if (exception.error == SocialAccountError.DISCORD_DIRECT_MESSAGE_BLOCKED) {
                throw BusinessException(error = NotificationError.RECIPIENT_UNREACHABLE, cause = exception)
            }
            throw exception
        }
    }
}
