package kr.byeongmin.watchtower.domain.auth.repository

import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.global.repository.BaseRepository

interface NaverOAuthRepository : BaseRepository<NaverOAuth, Long> {
    fun findFirstByProviderIdAndDeletedAtIsNullAndMemberDeletedAtIsNull(providerId: String): NaverOAuth?

    fun findFirstByMemberIdAndDeletedAtIsNull(memberId: Long): NaverOAuth?
}
