package kr.byeongmin.watchtower.global.error

/**
 * 인증 오류
 *
 * 글로벌에서 참조하고 있기 때문에 domain 패키지로 못옮김(global -> domain 참조 금지 컨벤션)
 */
enum class AuthError(
    override val statusCode: String,
    override val message: String
) : Error {
    UNAUTHORIZED("AUT_001", "인증이 필요합니다."),
    ACCESS_DENIED("AUT_002", "권한이 부족합니다."),
    INVALID_OAUTH_STATE("AUT_005", "유효하지 않은 로그인 요청입니다."),
    ALREADY_DELETED_ACCOUNT("AUT_006", "이미 탈퇴한 계정입니다."),
    BANNED_ACCOUNT("AUT_007", "정지된 계정입니다."),
    ACCOUNT_NOT_FOUND("AUT_008", "계정을 찾을 수 없습니다")
    ;

    override fun toString(): String {
        return "AuthError(statusCode='$statusCode', message='$message')"
    }
}
