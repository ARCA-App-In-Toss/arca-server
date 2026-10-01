package com.arca.global.config

import com.arca.global.infra.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD
import org.springframework.http.HttpHeaders.ORIGIN
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.options

@IntegrationTest
@AutoConfigureMockMvc
class CorsConfigTest(
    private val mockMvc: MockMvc,

    private val corsProperties: CorsProperties
) {

    @Nested
    inner class 허용된_origin에서_요청하면 {

        private val allowedOrigin = corsProperties.allowedOrigins.first()

        @Test
        fun preflight가_통과하고_요청한_origin을_그대로_돌려준다() {
            //when
            val response = mockMvc.options(PATH) {
                header(ORIGIN, allowedOrigin)
                header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
            }.andReturn().response

            //then
            assertThat(response.status).isEqualTo(200)
            assertThat(response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(allowedOrigin)
        }

        @Test
        fun preflight에서_멱등성_키_헤더를_허용한다() {
            //when
            val response = mockMvc.options(PATH) {
                header(ORIGIN, allowedOrigin)
                header(ACCESS_CONTROL_REQUEST_METHOD, "POST")
                header(ACCESS_CONTROL_REQUEST_HEADERS, IDEMPOTENCY_KEY)
            }.andReturn().response

            //then
            assertThat(response.status).isEqualTo(200)
            assertThat(response.getHeader(ACCESS_CONTROL_ALLOW_HEADERS)).contains(IDEMPOTENCY_KEY)
        }

        @Test
        fun 계약에_없는_메서드의_preflight는_거절된다() {
            //when
            val response = mockMvc.options(PATH) {
                header(ORIGIN, allowedOrigin)
                header(ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
            }.andReturn().response

            //then
            assertThat(response.status).isEqualTo(403)
        }

        @Test
        fun 실제_요청의_응답에서_Retry_After_헤더를_노출한다() {
            //when
            val response = mockMvc.get(PATH) {
                header(ORIGIN, allowedOrigin)
            }.andReturn().response

            //then
            assertThat(response.status).isEqualTo(200)
            assertThat(response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(allowedOrigin)
            assertThat(response.getHeader(ACCESS_CONTROL_EXPOSE_HEADERS)).contains(RETRY_AFTER)
        }
    }

    @Nested
    inner class 허용되지_않은_origin에서_요청하면 {

        @Test
        fun preflight가_거절된다() {
            //when
            val response = mockMvc.options(PATH) {
                header(ORIGIN, DISALLOWED_ORIGIN)
                header(ACCESS_CONTROL_REQUEST_METHOD, "GET")
            }.andReturn().response

            //then
            assertThat(response.status).isEqualTo(403)
            assertThat(response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN)).isNull()
        }

        @Test
        fun 실제_요청이_거절된다() {
            //when
            val response = mockMvc.get(PATH) {
                header(ORIGIN, DISALLOWED_ORIGIN)
            }.andReturn().response

            //then
            assertThat(response.status).isEqualTo(403)
            assertThat(response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN)).isNull()
        }
    }

    @Test
    fun allowlist에_wildcard_origin을_두지_않는다() {
        //then
        assertThat(corsProperties.allowedOrigins)
            .isNotEmpty()
            .doesNotContain(WILDCARD_ORIGIN)
    }

    companion object {
        private const val PATH = "/v1/test"
        private const val IDEMPOTENCY_KEY = "Idempotency-Key"
        private const val DISALLOWED_ORIGIN = "https://evil.example.com"
        private const val WILDCARD_ORIGIN = "*"
    }
}
