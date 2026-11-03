package kr.byeongmin.watchtower.external.slack.client

import com.slack.api.Slack
import com.slack.api.webhook.Payload
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * 운영자가 알아야 하는 서버 문제를 Slack Incoming Webhook 으로 보냄
 *
 * 오류를 처리하는 도중에 호출되므로 전송에 실패해도 예외를 던지지 않고 로그만 남김
 */
@Component
class SlackClient(
    environment: Environment,
    @Value("\${backend-slack-webhook-url}") private val webhookUrl: String
) {
    private val logger = KotlinLogging.logger {}
    private val slack = Slack.getInstance()
    private val activeProfile = environment.activeProfiles.first()

    fun sendErrorAlert(message: String, exception: Throwable) {
        val stackTrace = exception.stackTraceToString().take(STACK_TRACE_MAX_LENGTH)
        val payload = Payload.builder()
            .text(SensitiveValueMasker.mask("[backend:$activeProfile] $message\n```$stackTrace```"))
            .build()

        try {
            val response = slack.send(webhookUrl, payload)
            if (response.code != 200) {
                logger.error { "Slack 알림 전송 실패. code=${response.code}, body=${response.body}" }
            }
        } catch (sendException: Exception) {
            logger.error(sendException) { "Slack 알림 전송 실패" }
        }
    }

    companion object {
        /** Slack 메시지 본문 한도(40,000자)보다 충분히 짧게 잘라 원인이 되는 앞부분만 보냄 */
        const val STACK_TRACE_MAX_LENGTH = 10_000
    }
}
