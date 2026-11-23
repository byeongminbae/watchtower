package kr.byeongmin.watchtower.domain.auth.accessor

import kr.byeongmin.watchtower.domain.auth.entity.SocialAccount
import kr.byeongmin.watchtower.domain.auth.repository.SocialAccountRepository
import org.springframework.stereotype.Component

@Component
class SocialAccountAccessor(
    private val socialAccountRepository: SocialAccountRepository
) {
    fun findSocialAccountsOfMember(memberId: Long): List<SocialAccount> {
        return socialAccountRepository.findAllByMemberIdAndDeletedAtIsNull(memberId)
    }
}
