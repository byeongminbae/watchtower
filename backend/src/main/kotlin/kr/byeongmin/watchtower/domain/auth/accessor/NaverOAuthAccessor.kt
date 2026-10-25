package kr.byeongmin.watchtower.domain.auth.accessor

import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.domain.auth.repository.NaverOAuthRepository
import org.springframework.stereotype.Component

@Component
class NaverOAuthAccessor(
    private val naverOAuthRepository: NaverOAuthRepository
) {
    fun findByProviderId(providerId: String): NaverOAuth? {
        return naverOAuthRepository.findFirstByProviderIdAndDeletedAtIsNullAndMemberDeletedAtIsNull(providerId)
    }

    fun findByMemberId(memberId: Long): NaverOAuth? {
        return naverOAuthRepository.findFirstByMemberIdAndDeletedAtIsNull(memberId)
    }

    fun save(naverOAuth: NaverOAuth): NaverOAuth {
        return naverOAuthRepository.save(naverOAuth)
    }
}
