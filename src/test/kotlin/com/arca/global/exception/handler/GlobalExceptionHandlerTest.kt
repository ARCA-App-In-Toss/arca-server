package com.arca.global.exception.handler

import com.arca.global.infra.IntegrationTest
import com.arca.global.infra.TestController.Companion.RETRY_AFTER_SECONDS
import com.arca.global.infra.TestController.Companion.TICKET_ID
import com.arca.global.infra.TestController.Companion.UNEXPECTED_MESSAGE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

@IntegrationTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest(
    private val mockMvc: MockMvc,

    private val jsonMapper: JsonMapper
) {

    @Nested
    inner class RestApiException을_던지면 {

        @Test
        fun code에_고정된_상태와_envelope으로_응답한다() {
            //when
            val response = mockMvc.get("$PATH/exceptions/plain").andReturn().response

            //then
            val error = errorOf(response)
            assertThat(response.status).isEqualTo(404)
            assertThat(error.get("code").asString()).isEqualTo("ANSWER_NOT_FOUND")
            assertThat(error.get("category").asString()).isEqualTo("VALIDATION")
            assertThat(error.get("requestId").asString()).isNotBlank()
        }

        @Test
        fun recovery가_없는_code는_계약의_필수_필드만_내보낸다() {
            //when
            val response = mockMvc.get("$PATH/exceptions/plain").andReturn().response

            //then
            assertThat(bodyOf(response).propertyNames()).containsExactly("error")
            assertThat(errorOf(response).propertyNames()).containsExactly("code", "category", "requestId")
            assertThat(response.getHeader(RETRY_AFTER)).isNull()
        }

        @Test
        fun 세션_복구_recovery를_내보낸다() {
            //when
            val response = mockMvc.get("$PATH/exceptions/session-recovery").andReturn().response

            //then
            val recovery = errorOf(response).get("recovery")
            assertThat(response.status).isEqualTo(401)
            assertThat(recovery.propertyNames()).containsExactly("kind", "recoveryAllowed")
            assertThat(recovery.get("kind").asString()).isEqualTo("REESTABLISH_SESSION")
            assertThat(recovery.get("recoveryAllowed").asBoolean()).isTrue()
        }

        @Test
        fun 런타임_값을_실은_recovery를_내보낸다() {
            //when
            val response = mockMvc.get("$PATH/exceptions/command-pending").andReturn().response

            //then
            val recovery = errorOf(response).get("recovery")
            assertThat(response.status).isEqualTo(409)
            assertThat(recovery.propertyNames()).containsExactly("kind", "ticketId")
            assertThat(recovery.get("kind").asString()).isEqualTo("QUERY_COMMAND")
            assertThat(recovery.get("ticketId").asString()).isEqualTo(TICKET_ID)
        }

        @Test
        fun retryAfterSeconds와_Retry_After_헤더가_같은_값이다() {
            //when
            val response = mockMvc.get("$PATH/exceptions/rate-limited").andReturn().response

            //then
            val error = errorOf(response)
            assertThat(response.status).isEqualTo(429)
            assertThat(error.get("category").asString()).isEqualTo("RATE_LIMIT")
            assertThat(error.get("retryAfterSeconds").asInt()).isEqualTo(RETRY_AFTER_SECONDS)
            assertThat(response.getHeader(RETRY_AFTER)).isEqualTo(RETRY_AFTER_SECONDS.toString())
        }

        @Test
        fun JSON을_받지_않는_요청에도_envelope으로_응답한다() {
            //when
            val response = mockMvc.get("$PATH/exceptions/plain") {
                accept = TEXT_HTML
            }.andReturn().response

            //then
            assertThat(response.status).isEqualTo(404)
            assertThat(errorOf(response).get("code").asString()).isEqualTo("ANSWER_NOT_FOUND")
        }

        @Test
        fun 요청마다_다른_requestId를_내보낸다() {
            //when
            val first = mockMvc.get("$PATH/exceptions/plain").andReturn().response
            val second = mockMvc.get("$PATH/exceptions/plain").andReturn().response

            //then
            assertThat(errorOf(first).get("requestId").asString())
                .isNotEqualTo(errorOf(second).get("requestId").asString())
        }
    }

    @Nested
    inner class 요청_형식이_잘못되면_INVALID_REQUEST로_응답한다 {

        @Test
        fun JSON을_파싱할_수_없을_때() {
            //when
            val response = postJson("{")

            //then
            assertInvalidRequest(response)
        }

        @Test
        fun 필수_필드가_없을_때() {
            //when
            val response = postJson("{}")

            //then
            assertInvalidRequest(response)
        }

        @Test
        fun 필드_타입이_다를_때() {
            //when
            val response = postJson("""{"name": {"nested": 1}}""")

            //then
            assertInvalidRequest(response)
        }

        @Test
        fun 계약에_없는_필드가_있을_때() {
            //when
            val response = postJson("""{"name": "아르카", "unknown": 1}""")

            //then
            assertInvalidRequest(response)
        }

        @Test
        fun Bean_Validation에_실패할_때() {
            //when
            val response = postJson("""{"name": " "}""")

            //then
            assertInvalidRequest(response)
        }

        @Test
        fun 필수_헤더가_없을_때() {
            //when
            val response = mockMvc.get("$PATH/items/1").andReturn().response

            //then
            assertInvalidRequest(response)
        }

        @Test
        fun 헤더_형식이_다를_때() {
            //when
            val response = mockMvc.get("$PATH/items/1") {
                header(IDEMPOTENCY_KEY, "not-a-uuid")
            }.andReturn().response

            //then
            assertInvalidRequest(response)
        }

        @Test
        fun 경로_변수_타입이_다를_때() {
            //when
            val response = mockMvc.get("$PATH/items/abc") {
                header(IDEMPOTENCY_KEY, IDEMPOTENCY_KEY_VALUE)
            }.andReturn().response

            //then
            assertInvalidRequest(response)
        }

        @Test
        fun 없는_경로일_때() {
            //when
            val response = mockMvc.get("/v1/unknown").andReturn().response

            //then
            assertInvalidRequest(response)
        }

        @Test
        fun 지원하지_않는_메서드일_때() {
            //when
            val response = mockMvc.delete(PATH).andReturn().response

            //then
            assertInvalidRequest(response)
        }

        @Test
        fun 응답으로_JSON을_받지_않을_때() {
            //when
            val response = mockMvc.get(PATH) {
                accept = TEXT_HTML
            }.andReturn().response

            //then
            assertInvalidRequest(response)
        }

        @Test
        fun Content_Type이_JSON이_아닐_때() {
            //when
            val response = mockMvc.post(PATH) {
                contentType = TEXT_PLAIN
                content = "name"
            }.andReturn().response

            //then
            assertInvalidRequest(response)
        }
    }

    @Nested
    inner class 처리되지_않은_예외가_나면 {

        @Test
        fun INTERNAL_ERROR로_응답한다() {
            //when
            val response = mockMvc.get("$PATH/exceptions/unexpected").andReturn().response

            //then
            val error = errorOf(response)
            assertThat(response.status).isEqualTo(500)
            assertThat(error.get("code").asString()).isEqualTo("INTERNAL_ERROR")
            assertThat(error.get("category").asString()).isEqualTo("MAINTENANCE")
        }

        @Test
        fun 원인을_응답에_노출하지_않는다() {
            //when
            val response = mockMvc.get("$PATH/exceptions/unexpected").andReturn().response

            //then
            assertThat(errorOf(response).propertyNames()).containsExactly("code", "category", "requestId")
            assertThat(response.contentAsString).doesNotContain(UNEXPECTED_MESSAGE)
        }
    }

    private fun postJson(json: String): MockHttpServletResponse {
        return mockMvc.post(PATH) {
            contentType = APPLICATION_JSON
            content = json
        }.andReturn().response
    }

    private fun bodyOf(response: MockHttpServletResponse): JsonNode {
        return jsonMapper.readTree(response.contentAsString)
    }

    private fun errorOf(response: MockHttpServletResponse): JsonNode {
        return bodyOf(response).get("error")
    }

    private fun assertInvalidRequest(response: MockHttpServletResponse) {
        val error = errorOf(response)
        assertThat(response.status).isEqualTo(400)
        assertThat(error.propertyNames()).containsExactly("code", "category", "requestId")
        assertThat(error.get("code").asString()).isEqualTo("INVALID_REQUEST")
        assertThat(error.get("category").asString()).isEqualTo("VALIDATION")
    }

    companion object {
        private const val PATH = "/v1/test"
        private const val IDEMPOTENCY_KEY = "Idempotency-Key"
        private const val IDEMPOTENCY_KEY_VALUE = "3f2b8c1e-6d4a-4f0b-9c7e-1a2b3c4d5e6f"
    }
}
