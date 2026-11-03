package kr.byeongmin.watchtower.global.exception

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.byeongmin.watchtower.external.slack.client.SlackClient
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView

/**
 * [GlobalExceptionHandler] 와 Spring MVC 기본 처리기가 모두 처리하지 못한 예외를 운영자에게 알림
 *
 * 가장 마지막 순서라서 잘못된 요청 형식처럼 Spring 이 4xx 로 처리하는 예외는 여기까지 오지 않음.
 * 응답은 바꾸지 않고 null 을 돌려줘 기존처럼 500 오류 응답이 나가게 함
 *
 * 여기까지 오는 예외 예시
 * - DB 연결 실패, 커넥션 풀 고갈: CannotCreateTransactionException
 * - 유니크 제약 등 DB 제약 위반: DataIntegrityViolationException
 * - 네이버 API 연결 실패나 타임아웃: ResourceAccessException ([kr.byeongmin.watchtower.global.config.RestClientConfig] 는 4xx, 5xx 응답만 변환함)
 * - 코드 버그: NullPointerException, IllegalStateException, IllegalArgumentException
 *
 * 여기까지 오지 않는 예외 예시
 * - [BusinessException]: [GlobalExceptionHandler] 가 처리함
 * - 검증 실패, 깨진 JSON, 없는 경로, 지원하지 않는 HTTP 메서드: Spring MVC 가 4xx 로 처리함
 * - 인증, 권한 거부: Spring Security 가 처리함
 */
@Component
class UnhandledExceptionAlertResolver(
    private val slackClient: SlackClient
) : HandlerExceptionResolver, Ordered {
    override fun resolveException(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any?,
        exception: Exception
    ): ModelAndView? {
        slackClient.sendErrorAlert("처리하지 못한 예외. ${request.method} ${request.requestURI}", exception)
        return null
    }

    override fun getOrder(): Int {
        return Ordered.LOWEST_PRECEDENCE
    }
}
