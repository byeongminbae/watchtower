package kr.byeongmin.watchtower.global.config

import io.swagger.v3.oas.annotations.Operation as ApiOperation
import io.swagger.v3.oas.annotations.security.SecurityRequirement as ApiSecurityRequirement
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.security.SessionMemberLoader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 스웨거 설정 클래스
 */
@Configuration
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI {
        val sessionCookie = SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .`in`(SecurityScheme.In.COOKIE)
            .name("JSESSIONID")
        val csrfToken = SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .`in`(SecurityScheme.In.HEADER)
            .name("X-CSRF-TOKEN")
            .description("GET /api/v1/auth/csrf 응답의 token을 입력. 로그인과 로그아웃 후에는 다시 조회해서 갱신")

        return OpenAPI().components(
            Components().addSecuritySchemes(
                "Session Cookie",
                sessionCookie
            ).addSecuritySchemes(
                "CSRF Token",
                csrfToken
            )
        ).addSecurityItem(
            SecurityRequirement().addList("Session Cookie").addList("CSRF Token")
        ).path(
            "/api/v1/auth/logout",
            PathItem().delete(
                Operation()
                    .operationId("logout")
                    .summary("현재 로그인한 세션 삭제")
                    .tags(listOf("auth-controller"))
                    .security(listOf(SecurityRequirement().addList("CSRF Token")))
                    .responses(
                        ApiResponses().addApiResponse(
                            "200",
                            ApiResponse().description("로그아웃 성공").content(
                                Content().addMediaType(
                                    "application/json",
                                    MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/SuccessResponse"))
                                )
                            )
                        )
                    )
            )
        )
    }

    @Profile("local")
    @RestController
    class TestSessionController(
        private val sessionMemberLoader: SessionMemberLoader,
        private val sessionAuthenticationStrategy: SessionAuthenticationStrategy,
        private val securityContextRepository: SecurityContextRepository
    ) {
        @ApiOperation(
            summary = "로컬 테스트용 로그인 세션 발급",
            description = "기존 회원 ID로 로그인. 먼저 /api/v1/auth/csrf의 token을 Authorize의 CSRF Token에 입력하고, 발급 후 토큰을 다시 조회해서 갱신",
            security = [ApiSecurityRequirement(name = "CSRF Token")]
        )
        @PostMapping("/api/v1/auth/test-session")
        fun issueTestSession(
            @RequestParam("memberId") memberId: Long,
            request: HttpServletRequest,
            response: HttpServletResponse
        ): SuccessResponse {
            val authentication = sessionMemberLoader.getMemberPrinciple(memberId).to()
            sessionAuthenticationStrategy.onAuthentication(authentication, request, response)

            val securityContext = SecurityContextHolder.createEmptyContext()
            securityContext.authentication = authentication
            SecurityContextHolder.setContext(securityContext)
            securityContextRepository.saveContext(securityContext, request, response)
            return SuccessResponse()
        }
    }
}
