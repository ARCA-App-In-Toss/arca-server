package com.arca.global.infra

import com.arca.auth.domain.AuthSession
import com.arca.global.annotation.Auth
import com.arca.global.infra.AuthInterceptor.Companion.AUTH_SESSION_ATTRIBUTE
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(methodParameter: MethodParameter): Boolean {
        return methodParameter.hasParameterAnnotation(Auth::class.java) &&
                methodParameter.parameterType == AuthSession::class.java
    }

    override fun resolveArgument(
        methodParameter: MethodParameter,
        modelAndViewContainer: ModelAndViewContainer?,
        nativeWebRequest: NativeWebRequest,
        webDataBinderFactory: WebDataBinderFactory?
    ): AuthSession {
        val authSession = nativeWebRequest.getAttribute(AUTH_SESSION_ATTRIBUTE, SCOPE_REQUEST) as? AuthSession

        return checkNotNull(authSession){
            "@Auth requires @AllowedSessionModes on ${methodParameter.method?.name}"
        }
    }
}