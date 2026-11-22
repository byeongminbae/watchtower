package kr.byeongmin.watchtower.global.security

import jakarta.servlet.http.HttpServletResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.logout.LogoutFilter
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextHolderFilter
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository
import org.springframework.security.web.savedrequest.NullRequestCache
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val objectMapper: ObjectMapper,
    @Value("\${watchtower.frontend-url}") private val frontendUrl: String
) {
    private companion object {
        const val PREFLIGHT_CACHE_SECONDS = 3600L
        const val LOGIN_CALLBACK_PATH = "/login/callback"
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        sessionMemberLoader: SessionMemberLoader,
        securityContextRepository: SecurityContextRepository,
        csrfTokenRepository: CsrfTokenRepository,
        securityContextLogoutHandler: SecurityContextLogoutHandler,
        oauth2UserService: OAuth2UserService<OAuth2UserRequest, OAuth2User>
    ): SecurityFilterChain {
        val securityExceptionHandlerFilter = SecurityExceptionHandlerFilter(objectMapper)

        http {
            cors { configurationSource = corsConfigurationSource() }
            csrf { this.csrfTokenRepository = csrfTokenRepository }
            logout {
                logoutRequestMatcher = pathPattern(HttpMethod.DELETE, "/api/v1/auth/logout")
                deleteCookies("JSESSIONID")
                logoutSuccessHandler = LogoutSuccessHandler { _, response, _ -> writeSuccessResponse(response) }
            }
            oauth2Login {
                authorizationEndpoint { baseUri = "/api/v1/oauth2/authorization" }
                redirectionEndpoint { baseUri = "/api/v1/oauth2/callback/*" }
                userInfoEndpoint { userService = oauth2UserService }
                authorizedClientRepository = HttpSessionOAuth2AuthorizedClientRepository()
                defaultSuccessUrl("$frontendUrl$LOGIN_CALLBACK_PATH", true)
                authenticationFailureHandler = AuthenticationFailureHandler { _, response, exception ->
                    response.sendRedirect(toLoginFailureUrl(exception))
                }
            }
            requestCache { requestCache = NullRequestCache() }
            securityContext { this.securityContextRepository = securityContextRepository }
            exceptionHandling {
                authenticationEntryPoint = securityExceptionHandlerFilter
                accessDeniedHandler = securityExceptionHandlerFilter
            }
            authorizeHttpRequests { authorize(anyRequest, permitAll) }
            addFilterAfter<SecurityContextHolderFilter>(securityExceptionHandlerFilter)
            addFilterAfter<LogoutFilter>(SessionAuthenticationFilter(sessionMemberLoader, securityContextLogoutHandler))
        }

        return http.build()
    }

    private fun writeSuccessResponse(response: HttpServletResponse) {
        with(response) {
            contentType = MediaType.APPLICATION_JSON_VALUE
            characterEncoding = StandardCharsets.UTF_8.name()
            objectMapper.writeValue(writer, SuccessResponse())
        }
    }

    private fun toLoginFailureUrl(exception: AuthenticationException): String {
        val errorCode = (exception as? OAuth2AuthenticationException)?.error?.errorCode ?: OAuth2ErrorCodes.SERVER_ERROR
        return UriComponentsBuilder.fromUriString("$frontendUrl$LOGIN_CALLBACK_PATH")
            .queryParam("error", errorCode)
            .toUriString()
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
                    "https://dev.watchtower.boo",
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
