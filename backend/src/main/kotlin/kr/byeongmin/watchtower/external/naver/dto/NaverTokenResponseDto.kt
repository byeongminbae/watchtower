package kr.byeongmin.watchtower.external.naver.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class NaverTokenResponseDto(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("refresh_token")
    val refreshToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("expires_in")
    val expiresIn: Long
)
