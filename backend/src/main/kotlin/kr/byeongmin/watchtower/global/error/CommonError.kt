package kr.byeongmin.watchtower.global.error

enum class CommonError(
    override val statusCode: String,
    override val message: String
) : Error {
    INVALID_REQUEST("REQ_000", "잘못된 요청입니다."),
    INVALID_INPUT_VALUE("REQ_001", "입력값이 올바르지 않습니다."),

    PERMISSION_DENIED("PER_001", "접근 권한이 없습니다."),

    RESOURCE_NOT_FOUND("RES_001", "리소스를 찾을 수 없습니다."),
    RESOURCE_CONFLICT("RES_002", "이미 존재하는 리소스입니다."),

    INTERNAL_SERVER_ERROR("SER_000", "서버 내부 오류가 발생했습니다.");

    override fun toString(): String {
        return "CommonError(statusCode='$statusCode', message='$message')"
    }
}