package kr.byeongmin.watchtower.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 스웨거 설정 클래스
 *
 * JWT Bearer 인증 스키마와 전역 보안 요구 사항을 등록함
 * */
@Configuration
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI {
        val authorizationBearer = SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")

        return OpenAPI().components(
            Components().addSecuritySchemes(
                "Authorization Bearer",
                authorizationBearer
            )
        ).addSecurityItem(
            SecurityRequirement().addList("Authorization Bearer")
        )
    }
}
