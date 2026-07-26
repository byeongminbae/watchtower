package kr.byeongmin.watchtower.domain.auth.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kr.byeongmin.watchtower.domain.member.entity.Member
import java.time.LocalDateTime

@Entity
@DiscriminatorValue("NAVER")
@Table(name = "naver_oauth")
class NaverOAuth(
    member: Member,
    providerId: String,
    private val accessToken: String,
    private val refreshToken: String,
    private val expiredAt: LocalDateTime
) : OAuth(member, providerId)
