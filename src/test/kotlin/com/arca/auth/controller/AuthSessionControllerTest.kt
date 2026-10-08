package com.arca.auth.controller

import com.arca.auth.domain.SessionMode.GUEST
import com.arca.auth.infra.AccessTokenHasher
import com.arca.auth.infra.FakeAnonymousKeyVerifier.Companion.INVALID_ANONYMOUS_KEY
import com.arca.auth.repository.AuthSessionRepository
import com.arca.auth.service.AuthSessionService
import com.arca.global.infra.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.time.Instant

@IntegrationTest
@AutoConfigureMockMvc
class AuthSessionControllerTest(
    private val authSessionService: AuthSessionService,

    private val authSessionRepository: AuthSessionRepository,

    private val mockMvc: MockMvc,
    private val accessTokenHasher: AccessTokenHasher,
    private val jsonMapper: JsonMapper
) {

    @Nested
    inner class 유효한_익명_키로_교환하면 {

        @Test
        fun GUEST_세션을_201로_발급한다() {
            //when
            val response = postSession(bodyOf(VALID_ANONYMOUS_KEY))

            //then
            val body = jsonMapper.readTree(response.contentAsString)
            assertThat(response.status).isEqualTo(201)
            assertThat(body.get("accessToken").asString()).isNotEmpty()
            assertThat(body.get("context").get("mode").asString()).isEqualTo(GUEST.name)
            assertThat(body.get("context").has("member")).isTrue()
            assertThat(body.get("context").get("member").isNull).isTrue()
        }

        @Test
        fun 만료_시각은_발급_시각에서_24시간_뒤다() {
            //given
            val before = Instant.now()

            //when
            val response = postSession(bodyOf(VALID_ANONYMOUS_KEY))

            //then
            val after = Instant.now()
            val expiresAt = Instant.parse(jsonMapper.readTree(response.contentAsString).get("expiresAt").asString())
            assertThat(expiresAt).isBetween(before.plus(TTL), after.plus(TTL))
        }

        @Test
        fun 현재_필수_정책_목록을_담는다() {
            //when
            val response = postSession(bodyOf(VALID_ANONYMOUS_KEY))

            //then
            val consentPolicies = jsonMapper.readTree(response.contentAsString).get("consentPolicies").toList()
            assertThat(consentPolicies).extracting<String> { it.get("policyId").asString() }
                .containsExactly(TERMS_OF_SERVICE_ID, PRIVACY_POLICY_ID)
            assertThat(consentPolicies).allSatisfy { consentPolicy ->
                assertThat(consentPolicy.propertyNames()).containsExactlyInAnyOrder(*CONSENT_POLICY_FIELDS)
                assertThat(consentPolicy.get("version").asString()).isNotEmpty()
                assertThat(consentPolicy.get("title").asString()).isNotEmpty()
                assertThat(consentPolicy.get("url").asString()).startsWith(HTTPS_PREFIX)
                assertThat(consentPolicy.get("required").asBoolean()).isTrue()
            }
        }

        @Test
        fun 걷어낸_필드는_내보내지_않는다() {
            //when
            val response = postSession(bodyOf(VALID_ANONYMOUS_KEY))

            //then
            val body = jsonMapper.readTree(response.contentAsString)
            assertThat(body.propertyNames()).containsExactlyInAnyOrder(*RESPONSE_FIELDS)
            assertThat(body.get("context").propertyNames()).containsExactlyInAnyOrder(*CONTEXT_FIELDS)
        }

        @Test
        fun 발급된_토큰은_회원_없는_GUEST_세션으로_인증된다() {
            //when
            val response = postSession(bodyOf(VALID_ANONYMOUS_KEY))

            //then
            val accessToken = jsonMapper.readTree(response.contentAsString).get("accessToken").asString()
            val authSession = authSessionService.authenticate(accessToken)
            assertThat(authSession.sessionMode).isEqualTo(GUEST)
            assertThat(authSession.memberId).isNull()
            assertThat(authSession.tokenHash).isEqualTo(accessTokenHasher.hash(accessToken))
        }

        @Test
        fun 다시_교환하면_새_토큰을_발급하고_이전_토큰은_유효하다() {
            //given
            val firstToken = accessTokenOf(postSession(bodyOf(VALID_ANONYMOUS_KEY)))

            //when
            val secondToken = accessTokenOf(postSession(bodyOf(VALID_ANONYMOUS_KEY)))

            //then
            assertThat(secondToken).isNotEqualTo(firstToken)
            assertThat(authSessionService.authenticate(firstToken).isRevoked()).isFalse()
        }
    }

    @Nested
    inner class 무효한_익명_키로_교환하면 {

        @Test
        fun ANONYMOUS_KEY_INVALID로_응답하고_세션을_만들지_않는다() {
            //given
            val sessionCount = authSessionRepository.count()

            //when
            val response = postSession(bodyOf(INVALID_ANONYMOUS_KEY))

            //then
            assertError(response, 401, "ANONYMOUS_KEY_INVALID", "AUTH")
            assertThat(authSessionRepository.count()).isEqualTo(sessionCount)
        }
    }

    @Nested
    inner class 요청_형식이_잘못되면_INVALID_REQUEST {

        @Test
        fun anonymousKey가_없으면() {
            //when
            val response = postSession("{}")

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }

        @Test
        fun anonymousKey가_빈_문자열이면() {
            //when
            val response = postSession(bodyOf(""))

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }

        @Test
        fun anonymousKey가_null이면() {
            //when
            val response = postSession("{\"anonymousKey\": null}")

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }

        @Test
        fun 추가_필드가_있으면() {
            //when
            val response = postSession("{\"anonymousKey\": \"$VALID_ANONYMOUS_KEY\", \"extra\": 1}")

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }

        @Test
        fun JSON이_아니면() {
            //when
            val response = postSession("not-json")

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }
    }

    @Nested
    inner class 공백만_있는_익명_키는 {

        @Test
        fun 형식_검증을_통과해_검증기로_넘어간다() {
            //when
            val response = postSession(bodyOf(BLANK_ANONYMOUS_KEY))

            //then
            assertThat(response.status).isEqualTo(201)
        }
    }

    private fun postSession(content: String): MockHttpServletResponse {
        return mockMvc.post(SESSIONS_PATH) {
            contentType = APPLICATION_JSON
            this.content = content
        }.andReturn().response
    }

    private fun bodyOf(anonymousKey: String): String {
        return "{\"anonymousKey\": \"$anonymousKey\"}"
    }

    private fun accessTokenOf(response: MockHttpServletResponse): String {
        return jsonMapper.readTree(response.contentAsString).get("accessToken").asString()
    }

    private fun errorOf(response: MockHttpServletResponse): JsonNode {
        return jsonMapper.readTree(response.contentAsString).get("error")
    }

    private fun assertError(
        response: MockHttpServletResponse,
        status: Int,
        code: String,
        category: String
    ) {
        assertThat(response.status).isEqualTo(status)
        assertThat(errorOf(response).get("code").asString()).isEqualTo(code)
        assertThat(errorOf(response).get("category").asString()).isEqualTo(category)
    }

    companion object {
        private const val SESSIONS_PATH = "/v1/sessions"
        private const val VALID_ANONYMOUS_KEY = "valid-anonymous-key"
        private const val BLANK_ANONYMOUS_KEY = "   "
        private const val TERMS_OF_SERVICE_ID = "terms-of-service"
        private const val PRIVACY_POLICY_ID = "privacy-policy"
        private const val HTTPS_PREFIX = "https://"
        private val TTL = Duration.ofHours(24)
        private val RESPONSE_FIELDS = arrayOf("accessToken", "expiresAt", "context", "consentPolicies")
        private val CONTEXT_FIELDS = arrayOf("mode", "member")
        private val CONSENT_POLICY_FIELDS = arrayOf("policyId", "version", "title", "url", "required")
    }
}
