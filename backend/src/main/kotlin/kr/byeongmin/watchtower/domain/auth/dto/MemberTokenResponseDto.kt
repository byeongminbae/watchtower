package kr.byeongmin.watchtower.domain.auth.dto

/**
 * @property [accessTokenExpiresIn] 밀리초 단위
 * @property [refreshTokenExpiresIn] 밀리초 단위
 */
class MemberTokenResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long,
    val refreshTokenExpiresIn: Long
)