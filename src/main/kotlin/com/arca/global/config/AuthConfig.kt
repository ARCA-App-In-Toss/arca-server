package com.arca.global.config

import com.arca.global.infra.AuthArgumentResolver
import com.arca.global.infra.AuthInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AuthConfig(
    private val authInterceptor: AuthInterceptor,
    private val authArgumentResolver: AuthArgumentResolver
) : WebMvcConfigurer {

    override fun addInterceptors(interceptorRegistry: InterceptorRegistry) {
        interceptorRegistry.addInterceptor(authInterceptor)
            .addPathPatterns(PATH_PATTERN)
    }

    override fun addArgumentResolvers(argumentResolvers: MutableList<HandlerMethodArgumentResolver>) {
        argumentResolvers.add(authArgumentResolver)
    }

    companion object {
        private const val PATH_PATTERN = "/v1/**"
    }
}
