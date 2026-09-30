package com.arca.global.infra

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.logging.log4j.ThreadContext
import org.springframework.core.Ordered.HIGHEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(HIGHEST_PRECEDENCE + 1)
class RequestIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = UUID.randomUUID().toString()
        httpServletRequest.setAttribute(REQUEST_ID_ATTRIBUTE, requestId)
        ThreadContext.put(REQUEST_ID_ATTRIBUTE, requestId)

        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse)
        } finally {
            ThreadContext.remove(REQUEST_ID_ATTRIBUTE)
        }
    }

    companion object {
        const val REQUEST_ID_ATTRIBUTE = "requestId"
    }
}
