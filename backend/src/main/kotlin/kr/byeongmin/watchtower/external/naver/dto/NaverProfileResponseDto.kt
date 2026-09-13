package kr.byeongmin.watchtower.external.naver.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class NaverProfileResponseDto(
    @JsonProperty("resultcode")
    val resultCode: String,
    val message: String,
    val response: NaverProfileDetailDto
) {
    data class NaverProfileDetailDto(
        @JsonProperty("id")
        val providerId: String,
        val email: String,
        val nickname: String,
        @JsonProperty("profile_image")
        val profileImage: String
    )
}
