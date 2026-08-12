package kr.byeongmin.watchtower.domain.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class NaverTokenResponseExternalDto(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("refresh_token")
    val refreshToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("expires_in")
    val expiresIn: Long
)