package kr.byeongmin.watchtower.global.error

/**
 * 응답 본문에 노출할 애플리케이션 오류 인터페이스
 * @property [statusCode] 서비스 컨벤션에 맞는 오류 코드(HTTP 상태 코드가 아님)
 * @property [message] 프론트에 전달하는 오류 메시지
 * */
// TODO: message를 forUser forDebug 같이 유저가 보는 메시지랑 프론트 엔지니어에게 필요한 메시지 두개를 나누는건 어떤지? 근데 복잡도가 약간 올라가지 않을까? 고민해보자
interface Error {
    val statusCode: String
    val message: String

    /** 요청한 사용자가 아니라 서버나 외부 서비스 쪽 문제라서 운영자에게 Slack 알림을 보내야 하는 오류인지 여부 */
    fun isOperatorAlertRequired(): Boolean {
        return false
    }
}