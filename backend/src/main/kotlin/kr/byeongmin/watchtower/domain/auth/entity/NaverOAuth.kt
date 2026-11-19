package kr.byeongmin.watchtower.domain.auth.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kr.byeongmin.watchtower.domain.member.entity.Member

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
        fun of(
            member: Member,
            providerId: String,
            accessToken: String,
            refreshToken: String,
            expiresIn: Long
        ): NaverOAuth {
            return NaverOAuth(
                member = member,
                providerId = providerId,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = expiresIn
            )
        }
    }
}
