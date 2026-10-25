package kr.byeongmin.watchtower.global.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val objectMapper: ObjectMapper
) {
    private companion object {
        const val PREFLIGHT_CACHE_SECONDS = 3600L
    }

    /** JWT 검증보다 앞에 오류 처리 필터를 배치 */
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter
    ): SecurityFilterChain {
        val securityExceptionHandlerFilter = SecurityExceptionHandlerFilter(objectMapper)

        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(securityExceptionHandlerFilter)
                it.accessDeniedHandler(securityExceptionHandlerFilter)
            }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(securityExceptionHandlerFilter, JwtAuthenticationFilter::class.java)

        return http.build()
    }

    /** 프론트엔드 출처의 CORS 요청을 허용 */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins =
                listOf(
                    "http://localhost:3000",
                    "http://127.0.0.1:3000",
                    "https://front-dev.watchtower.boo",
                    "https://watchtower.boo"
                )
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type")
            allowCredentials = true
            maxAge = PREFLIGHT_CACHE_SECONDS
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
