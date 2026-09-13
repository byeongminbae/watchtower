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
            naverProfileResponseDto: NaverProfileResponseDto,
            naverTokenResponseDto: NaverTokenResponseDto
        ): NaverOAuth {
            return NaverOAuth(
                member = member,
                providerId = naverProfileResponseDto.response.providerId,
                accessToken = naverTokenResponseDto.accessToken,
                refreshToken = naverTokenResponseDto.refreshToken,
                expiredAt = naverTokenResponseDto.expiresIn
            )
        }
    }
}