package kr.byeongmin.watchtower.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** @Scheduled 메서드를 주기적으로 실행하는 설정 클래스 */
@Configuration
@EnableScheduling
class SchedulingConfig
