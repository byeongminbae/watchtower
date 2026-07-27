package kr.byeongmin.watchtower.domain.auth.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kr.byeongmin.watchtower.domain.member.entity.Member

@Entity
@DiscriminatorValue("NAVER")
@Table(name = "naver_oauth")
class NaverOAuth(
    member: Member,
    providerId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiredAt: Long
) : OAuth(member, providerId)
