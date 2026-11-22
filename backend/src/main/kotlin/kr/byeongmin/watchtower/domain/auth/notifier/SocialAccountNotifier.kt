package kr.byeongmin.watchtower.domain.auth.notifier

import kr.byeongmin.watchtower.domain.auth.entity.DiscordAccount

interface SocialAccountNotifier {
    fun send(discordAccount: DiscordAccount, content: String)
}
