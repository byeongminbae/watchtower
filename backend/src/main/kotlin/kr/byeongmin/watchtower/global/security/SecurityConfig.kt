package kr.byeongmin.watchtower.global.security

import kr.byeongmin.watchtower.global.dto.SuccessResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.logout.LogoutFilter
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextHolderFilter
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val objectMapper: ObjectMapper
) {
    private companion object {
        const val PREFLIGHT_CACHE_SECONDS = 3600L
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        sessionMemberLoader: SessionMemberLoader,
        securityContextRepository: SecurityContextRepository,
        csrfTokenRepository: CsrfTokenRepository,
        securityContextLogoutHandler: SecurityContextLogoutHandler
    ): SecurityFilterChain {
        val securityExceptionHandlerFilter = SecurityExceptionHandlerFilter(objectMapper)
        val sessionAuthenticationFilter = SessionAuthenticationFilter(
            sessionMemberLoader,
            securityContextLogoutHandler
        )

        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf {
                it.csrfTokenRepository(csrfTokenRepository)
                it.requireCsrfProtectionMatcher(
                    OrRequestMatcher(
                        CsrfFilter.DEFAULT_CSRF_MATCHER,
                        PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/v1/auth/naver/url")
                    )
                )
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout {
                it.logoutRequestMatcher(
                    PathPatternRequestMatcher.pathPattern(HttpMethod.DELETE, "/api/v1/auth/logout")
                )
                it.deleteCookies("JSESSIONID")
                it.logoutSuccessHandler { _, response, _ ->
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.characterEncoding = StandardCharsets.UTF_8.name()
                    objectMapper.writeValue(response.writer, SuccessResponse())
                }
            }
            .requestCache { it.disable() }
            .sessionManagement { it.requireExplicitAuthenticationStrategy(true) }
            .securityContext {
                it.securityContextRepository(securityContextRepository)
                it.requireExplicitSave(true)
            }
            .exceptionHandling {
                it.authenticationEntryPoint(securityExceptionHandlerFilter)
                it.accessDeniedHandler(securityExceptionHandlerFilter)
            }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .addFilterAfter(securityExceptionHandlerFilter, SecurityContextHolderFilter::class.java)
            .addFilterAfter(sessionAuthenticationFilter, LogoutFilter::class.java)

        return http.build()
    }

    @Bean
    fun securityContextRepository(): SecurityContextRepository {
        return HttpSessionSecurityContextRepository().apply {
            setDisableUrlRewriting(true)
        }
    }

    @Bean
    fun csrfTokenRepository(): CsrfTokenRepository {
        return HttpSessionCsrfTokenRepository()
    }

    @Bean
    fun authorizationRequestRepository(): AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
        return HttpSessionOAuth2AuthorizationRequestRepository()
    }

    @Bean
    fun sessionAuthenticationStrategy(csrfTokenRepository: CsrfTokenRepository): SessionAuthenticationStrategy {
        return CompositeSessionAuthenticationStrategy(
            listOf(
                ChangeSessionIdAuthenticationStrategy(),
                CsrfAuthenticationStrategy(csrfTokenRepository)
            )
        )
    }

    @Bean
    fun securityContextLogoutHandler(securityContextRepository: SecurityContextRepository): SecurityContextLogoutHandler {
        return SecurityContextLogoutHandler().apply {
            setSecurityContextRepository(securityContextRepository)
        }
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
            allowedHeaders = listOf("Content-Type", "X-CSRF-TOKEN")
            allowCredentials = true
            maxAge = PREFLIGHT_CACHE_SECONDS
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
