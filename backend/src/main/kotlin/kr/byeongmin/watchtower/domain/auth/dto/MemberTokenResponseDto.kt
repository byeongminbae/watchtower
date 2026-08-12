package kr.byeongmin.watchtower.domain.auth.dto

class MemberTokenResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiry: Long,
    val refreshTokenExpiry: Long
)