package com.arca.global.config

import com.arca.global.property.CorsProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig(
    private val corsProperties: CorsProperties
) : WebMvcConfigurer {

    override fun addCorsMappings(corsRegistry: CorsRegistry) {
        corsRegistry.addMapping(PATH_PATTERN)
            .allowedOrigins(*corsProperties.allowedOrigins.toTypedArray())
            .allowedHeaders(*ALLOWED_HEADERS)
            .allowedMethods(*ALLOWED_METHODS)
            .exposedHeaders(RETRY_AFTER)
            .allowCredentials(false)
    }

    companion object {
        private const val PATH_PATTERN = "/v1/**"
        private val ALLOWED_METHODS = arrayOf("GET", "POST", "PUT", "DELETE")
        private val ALLOWED_HEADERS = arrayOf(AUTHORIZATION, CONTENT_TYPE)
    }
}
