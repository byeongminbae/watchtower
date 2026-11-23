package kr.byeongmin.watchtower.domain.auth.repository

import kr.byeongmin.watchtower.domain.auth.entity.DiscordAccount
import kr.byeongmin.watchtower.global.repository.BaseRepository

interface DiscordAccountRepository : BaseRepository<DiscordAccount, Long> {
    fun findFirstByProviderIdAndDeletedAtIsNullAndMemberDeletedAtIsNull(providerId: String): DiscordAccount?

    fun findFirstByMemberIdAndDeletedAtIsNull(memberId: Long): DiscordAccount?
}
