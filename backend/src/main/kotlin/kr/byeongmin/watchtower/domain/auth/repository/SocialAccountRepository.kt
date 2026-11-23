package kr.byeongmin.watchtower.domain.auth.repository

import kr.byeongmin.watchtower.domain.auth.entity.SocialAccount
import kr.byeongmin.watchtower.global.repository.BaseRepository

interface SocialAccountRepository : BaseRepository<SocialAccount, Long> {
    fun findAllByMemberIdAndDeletedAtIsNull(memberId: Long): List<SocialAccount>
}
