package kr.byeongmin.watchtower.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
