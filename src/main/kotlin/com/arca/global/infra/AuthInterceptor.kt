package com.arca.global.infra

import com.arca.auth.service.AuthSessionService
import com.arca.global.annotation.AllowedSessionModes
import com.arca.global.exception.domain.ExceptionCode.SESSION_INVALID
import com.arca.global.exception.domain.ExceptionCode.SESSION_SCOPE_INSUFFICIENT
import com.arca.global.exception.domain.RestApiException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthInterceptor(
    private val authSessionService: AuthSessionService
): HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if(handler !is HandlerMethod){
            return true
        }

        val allowedSessionModes = handler.getMethodAnnotation(AllowedSessionModes::class.java)
            ?: return true

        val accessToken = extractAccessToken(request)
        val authSession = authSessionService.authenticate(accessToken)

        if(!authSession.isAllowed(allowedSessionModes.sessionModes)){
            throw RestApiException(SESSION_SCOPE_INSUFFICIENT)
        }

        request.setAttribute(AUTH_SESSION_ATTRIBUTE, authSession)

        return true
    }

    private fun extractAccessToken(request: HttpServletRequest): String {
        val authorization: String? = request.getHeader(AUTHORIZATION)

        if(authorization == null || !authorization.startsWith(BEARER_PREFIX, ignoreCase = true)){
            throw RestApiException(SESSION_INVALID)
        }

        val accessToken = authorization.substring(BEARER_PREFIX.length).trim()

        if(accessToken.isEmpty()){
            throw RestApiException(SESSION_INVALID)
        }

        return accessToken
    }

    companion object {
        const val AUTH_SESSION_ATTRIBUTE = "authSession"
        private const val BEARER_PREFIX = "Bearer "
    }
}