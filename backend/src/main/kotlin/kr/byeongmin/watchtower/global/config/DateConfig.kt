package kr.byeongmin.watchtower.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class DateConfig {
    @Bean
    fun date(): Date {
        return Date()
    }
}