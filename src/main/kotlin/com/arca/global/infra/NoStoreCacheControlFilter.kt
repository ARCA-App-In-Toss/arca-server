package com.arca.global.infra

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered.HIGHEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(HIGHEST_PRECEDENCE)
class NoStoreCacheControlFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        response.setHeader(CACHE_CONTROL, NO_STORE)
        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilterErrorDispatch(): Boolean {
        return false
    }

    companion object {
        private const val NO_STORE = "no-store"
    }
}
