package kr.byeongmin.watchtower.domain.member.sender

import kr.byeongmin.watchtower.domain.auth.accessor.DiscordAccountAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.external.discord.client.DiscordClient
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import org.springframework.stereotype.Component

@Component
class MemberNotificationSender(
    private val discordAccountAccessor: DiscordAccountAccessor,
    private val discordClient: DiscordClient
) {
    fun sendTestNotification(member: Member) {
        val discordAccount = discordAccountAccessor.getDiscordAccountOfMember(member.id.ifNullThrow())
        discordClient.sendMessage(
            discordAccount.directMessageChannelId,
            "### ${member.nickname} 님! 테스트 알림이 잘 도착했어요\n지켜보는 페이지에 변화가 생기면 이렇게 알려드릴게요."
        )
    }
}
