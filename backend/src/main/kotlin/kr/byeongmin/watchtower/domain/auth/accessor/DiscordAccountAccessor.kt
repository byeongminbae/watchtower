package kr.byeongmin.watchtower.domain.auth.accessor

import kr.byeongmin.watchtower.domain.auth.entity.DiscordAccount
import kr.byeongmin.watchtower.domain.auth.error.SocialAccountError
import kr.byeongmin.watchtower.domain.auth.repository.DiscordAccountRepository
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class DiscordAccountAccessor(
    private val discordAccountRepository: DiscordAccountRepository
) {
    fun findDiscordAccount(providerId: String): DiscordAccount? {
        return discordAccountRepository.findFirstByProviderIdAndDeletedAtIsNullAndMemberDeletedAtIsNull(
            providerId
        )
    }

    fun findDiscordAccountOfMember(memberId: Long): DiscordAccount? {
        return discordAccountRepository.findFirstByMemberIdAndDeletedAtIsNull(memberId)
    }

    fun getDiscordAccountOfMember(memberId: Long): DiscordAccount {
        return findDiscordAccountOfMember(memberId)
            ?: throw BusinessException(SocialAccountError.SOCIAL_ACCOUNT_NOT_LINKED)
    }

    fun save(discordAccount: DiscordAccount): DiscordAccount {
        return discordAccountRepository.save(discordAccount)
    }
}
