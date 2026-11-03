package kr.byeongmin.watchtower.global.config

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer
import tools.jackson.databind.module.SimpleModule
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

/**
 * 서버의 [LocalDateTime] 은 모두 UTC 이므로 응답에 UTC 표시(Z)를 붙여 클라이언트가 시간대를 추측하지 않게 하는 설정 클래스
 *
 * UTC 가 아닌 [LocalDateTime] 을 응답에 담으면 잘못된 시각으로 표시되니 [TimeProvider] 로 만든 시각만 담을것
 */
@Configuration
class JacksonConfig {
    @Bean
    fun utcLocalDateTimeCustomizer(): JsonMapperBuilderCustomizer {
        val utcFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .appendLiteral('Z')
            .toFormatter()
        val module = SimpleModule().addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer(utcFormatter))
        return JsonMapperBuilderCustomizer { builder -> builder.addModule(module) }
    }
}
