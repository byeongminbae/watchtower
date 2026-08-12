package kr.byeongmin.watchtower.global.error

enum class AuthError(
    override val statusCode: String,
    override val message: String
) : Error {
    UNAUTHORIZED("AUT_001", "인증이 필요합니다."),
    ACCESS_DENIED("AUT_002", "권한이 부족합니다."),
    INVALID_TOKEN("AUT_003", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN("AUT_004", "만료된 토큰입니다.");

    override fun toString(): String {
        return "AuthError(statusCode='$statusCode', message='$message')"
    }
}