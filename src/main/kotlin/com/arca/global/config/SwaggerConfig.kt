package com.arca.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openApi(): OpenAPI {
        val securityScheme = SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme(BEARER_SCHEME)
            .bearerFormat(BEARER_FORMAT)

        return OpenAPI()
            .info(Info().title(API_TITLE).version(API_VERSION))
            .components(Components().addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme))
    }

    companion object {
        const val SECURITY_SCHEME_NAME = "BearerAuth"
        private const val BEARER_SCHEME = "bearer"
        private const val BEARER_FORMAT = "opaque"
        private const val API_TITLE = "ARCA API"
        private const val API_VERSION = "v1"
    }
}
