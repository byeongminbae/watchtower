package kr.byeongmin.watchtower.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.time.temporal.TemporalAccessor
import java.util.Optional

/** 엔티티에 생성/수정 시각 감사 설정 클래스 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
class JpaAuditingConfig {
    /** 감사 이벤트가 발생할 때마다 [timeProvider]에서 시각을 얻음 */
    @Bean
    fun auditingDateTimeProvider(timeProvider: TimeProvider): DateTimeProvider {
        return DateTimeProvider { Optional.of<TemporalAccessor>(timeProvider.now()) }
    }
}
