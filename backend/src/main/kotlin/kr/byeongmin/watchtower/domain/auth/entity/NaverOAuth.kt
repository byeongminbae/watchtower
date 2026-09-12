package kr.byeongmin.watchtower.domain.auth.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.external.naver.dto.NaverTokenResponseDto

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
            naverMemberProfile: NaverProfileResponseDto,
            naverMemberToken: NaverTokenResponseDto
        ): NaverOAuth {
            return NaverOAuth(
                member = member,
                providerId = naverMemberProfile.response.providerId,
                accessToken = naverMemberToken.accessToken,
                refreshToken = naverMemberToken.refreshToken,
                expiredAt = naverMemberToken.expiresIn
            )
        }
    }
}