package kr.byeongmin.watchtower.domain.auth.dto

data class CsrfTokenResponseDto(
    val token: String,
    val headerName: String
)
