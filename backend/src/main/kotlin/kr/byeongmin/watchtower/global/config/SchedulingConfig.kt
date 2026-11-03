package kr.byeongmin.watchtower.global.config

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.byeongmin.watchtower.external.slack.client.SlackClient
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** @Scheduled 메서드를 주기적으로 실행하는 설정 클래스 */
@Configuration
@EnableScheduling
class SchedulingConfig {
    private val logger = KotlinLogging.logger {}

    /** 스케줄 작업이 예외로 끝나면 로그만 남기던 기본 처리 대신 운영자에게도 알림. 다음 실행은 그대로 예약됨 */
    @Bean
    fun scheduledTaskFailureAlertCustomizer(slackClient: SlackClient): ThreadPoolTaskSchedulerCustomizer {
        return ThreadPoolTaskSchedulerCustomizer { scheduler ->
            scheduler.setErrorHandler { exception ->
                logger.error(exception) { "스케줄 작업 실패" }
                slackClient.sendErrorAlert("스케줄 작업 실패", exception)
            }
        }
    }
}
