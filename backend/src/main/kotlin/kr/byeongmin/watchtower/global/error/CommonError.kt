package kr.byeongmin.watchtower.global.error

/**
 * 여러 도메인에서 공통으로 사용하는 오류
 *
 * 추상적인 표현만 가능해서 오류가 더 구체적인 표현이 필요하다면 해당 레이어에 [Error] 를 상속받아 규격에 맞춰 만들면 됨
 * */
enum class CommonError(
    override val statusCode: String,
    override val message: String
) : Error {
    INVALID_REQUEST("REQ_000", "잘못된 요청입니다."),
    INVALID_INPUT_VALUE("REQ_001", "입력값이 올바르지 않습니다."),

    PERMISSION_DENIED("PER_001", "접근 권한이 없습니다."),

    RESOURCE_NOT_FOUND("RES_001", "리소스를 찾을 수 없습니다."),
    RESOURCE_CONFLICT("RES_002", "이미 존재하는 리소스입니다."),
    RESOURCE_QUOTA_EXCEEDED("RES_003", "한도가 초과되었습니다."),
    RESOURCE_ALREADY_DELETED("RES_004", "이미 삭제된 엔티티 입니다."),

    EXTERNAL_API_ERROR("EXT_000", "외부 API에서 오류가 발생 했습니다."),

    INTERNAL_SERVER_ERROR("SER_000", "서버 내부 오류가 발생했습니다."),
    NULL_CASTING_ERROR("SER_001", "NULL 캐스팅 오류입니다.");

    override fun toString(): String {
        return "CommonError(statusCode='$statusCode', message='$message')"
    }
}