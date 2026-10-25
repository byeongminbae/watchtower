package kr.byeongmin.watchtower.global.exception

import kr.byeongmin.watchtower.global.error.Error
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 프로젝트 전역에서 공통적으로 사용하는 예외 래핑 클래스
 *
 * [BusinessException] 예외를 터트리면 [kr.byeongmin.watchtower.global.exception.GlobalExceptionHandler] 가 예외를 잡지만
 * 서블릿에 도달하기 전인 요청은 못잡으니 유의할것
 *
 * @property [error] 구체적으로 어떤 이유에서 예외를 터트렸는지에 대한 일종의 타입
 * */
class BusinessException(
    val error: Error,
    val timestamp: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
) : RuntimeException() {
    override fun toString(): String {
        return "BusinessException(error=$error, timestamp=$timestamp)"
    }
}