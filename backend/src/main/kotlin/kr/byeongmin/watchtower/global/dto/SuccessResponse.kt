package kr.byeongmin.watchtower.global.dto

import java.time.LocalDateTime
import java.time.ZoneId

/** 성공 응답이지만 클라이언트에게 전달해야 할 데이터가 없는 경우 사용하는 응답 클래스 */
class SuccessResponse : Response {
    override val isSuccess: Boolean = true
    override val timestamp: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
}
