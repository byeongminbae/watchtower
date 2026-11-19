package kr.byeongmin.watchtower.domain.auth.accessor

import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.domain.auth.repository.NaverOAuthRepository
import org.springframework.stereotype.Component

@Component
class NaverOAuthAccessor(
    private val naverOAuthRepository: NaverOAuthRepository
) {
    fun findNaverOAuth(providerId: String): NaverOAuth? {
        return naverOAuthRepository.findFirstByProviderIdAndDeletedAtIsNullAndMemberDeletedAtIsNull(providerId)
    }

    fun findMemberNaverOAuth(memberId: Long): NaverOAuth? {
        return naverOAuthRepository.findFirstByMemberIdAndDeletedAtIsNull(memberId)
    }

    fun save(naverOAuth: NaverOAuth): NaverOAuth {
        return naverOAuthRepository.save(naverOAuth)
    }
}
