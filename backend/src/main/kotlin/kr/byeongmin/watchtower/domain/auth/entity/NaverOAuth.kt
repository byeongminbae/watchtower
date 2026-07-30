package kr.byeongmin.watchtower.domain.auth.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kr.byeongmin.watchtower.domain.auth.dto.NaverProfileResponseExternalDto
import kr.byeongmin.watchtower.domain.auth.dto.NaverTokenResponseExternalDto
import kr.byeongmin.watchtower.domain.member.entity.Member

@Entity
@DiscriminatorValue("NAVER")
@Table(name = "naver_oauth")
class NaverOAuth private constructor(
    member: Member,
    providerId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiredAt: Long
) : OAuth(member, providerId) {
    companion object {
        fun from(
            member: Member,
            naverMemberProfile: NaverProfileResponseExternalDto,
            naverMemberToken: NaverTokenResponseExternalDto
        ): NaverOAuth {
            return NaverOAuth(
                member = member,
                providerId = naverMemberProfile.response.id,
                accessToken = naverMemberToken.accessToken,
                refreshToken = naverMemberToken.refreshToken,
                expiredAt = naverMemberToken.expiresIn
            )
        }
    }
}