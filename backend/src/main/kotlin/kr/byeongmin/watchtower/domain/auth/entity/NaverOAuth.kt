package kr.byeongmin.watchtower.domain.auth.entity

import kr.byeongmin.watchtower.domain.member.entity.Member
import java.time.LocalDateTime

class NaverOAuth(
    member: Member,
    providerId: String,
    private val accessToken: String,
    private val refreshToken: String,
    private val expiredAt: LocalDateTime
) : OAuth(member, providerId)