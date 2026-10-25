package kr.byeongmin.watchtower.domain.auth.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.external.naver.dto.NaverTokenResponseDto

/**
 * @property [expiresIn] 초 단위
 */
@Entity
@DiscriminatorValue("NAVER")
@Table(name = "naver_oauth")
class NaverOAuth private constructor(
    member: Member,
    providerId: String,
    accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
) : OAuth(member, providerId, accessToken) {
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
                expiresIn = naverTokenResponseDto.expiresIn
            )
        }
    }
}
