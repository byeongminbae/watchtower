package kr.byeongmin.watchtower.domain.auth.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kr.byeongmin.watchtower.domain.auth.notifier.SocialAccountNotifier
import kr.byeongmin.watchtower.domain.member.entity.Member

@Entity
@DiscriminatorValue("DISCORD")
@Table(name = "discord_account")
class DiscordAccount private constructor(
    member: Member,
    providerId: String,
    val directMessageChannelId: String
) : SocialAccount(member, providerId) {
    override fun sendNotification(notifier: SocialAccountNotifier, content: String) {
        notifier.send(this, content)
    }

    companion object {
        fun of(
            member: Member,
            providerId: String,
            directMessageChannelId: String
        ): DiscordAccount {
            val discordAccount = DiscordAccount(
                member = member,
                providerId = providerId,
                directMessageChannelId = directMessageChannelId
            )
            member.addSocialAccount(discordAccount)

            return discordAccount
        }
    }
}
