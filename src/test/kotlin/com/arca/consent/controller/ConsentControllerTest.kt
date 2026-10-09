package com.arca.consent.controller

import com.arca.auth.domain.SessionMode
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.GUEST
import com.arca.auth.infra.AuthSessionIssuer
import com.arca.global.infra.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

@IntegrationTest
@AutoConfigureMockMvc
class ConsentControllerTest(
    private val mockMvc: MockMvc,
    private val authSessionIssuer: AuthSessionIssuer,
    private val jsonMapper: JsonMapper
) {

    @Nested
    inner class 정책_목록을_조회하면 {

        @Test
        fun GUEST_세션은_현재_필수_정책_목록을_200으로_받는다() {
            //given
            val accessToken = issueToken(GUEST, null)

            //when
            val response = getConsentPolicies(accessToken)

            //then
            assertThat(response.status).isEqualTo(200)
            assertThat(consentPoliciesOf(response)).extracting<String> { it.get("policyId").asString() }
                .containsExactly(TERMS_OF_SERVICE_ID, PRIVACY_POLICY_ID)
        }

        @Test
        fun 정책마다_다섯_필드를_모두_담는다() {
            //given
            val accessToken = issueToken(GUEST, null)

            //when
            val response = getConsentPolicies(accessToken)

            //then
            val body = jsonMapper.readTree(response.contentAsString)
            assertThat(body.propertyNames()).containsExactly(CONSENT_POLICIES_FIELD)
            assertThat(consentPoliciesOf(response)).isNotEmpty().allSatisfy { consentPolicy ->
                assertThat(consentPolicy.propertyNames()).containsExactlyInAnyOrder(*CONSENT_POLICY_FIELDS)
                assertThat(consentPolicy.get("version").asString()).isNotEmpty()
                assertThat(consentPolicy.get("title").asString()).isNotEmpty()
                assertThat(consentPolicy.get("url").asString()).startsWith(HTTPS_PREFIX)
                assertThat(consentPolicy.get("required").asBoolean()).isTrue()
            }
        }

        @Test
        fun ACTIVE_세션도_조회할_수_있다() {
            //given
            val accessToken = issueToken(ACTIVE, MEMBER_ID)

            //when
            val response = getConsentPolicies(accessToken)

            //then
            assertThat(response.status).isEqualTo(200)
            assertThat(consentPoliciesOf(response)).hasSize(CONSENT_POLICY_COUNT)
        }

        @Test
        fun 세션이_없으면_SESSION_INVALID() {
            //when
            val response = mockMvc.get(CONSENT_POLICIES_PATH).andReturn().response

            //then
            assertThat(response.status).isEqualTo(401)
            assertThat(jsonMapper.readTree(response.contentAsString).get("error").get("code").asString())
                .isEqualTo("SESSION_INVALID")
        }
    }

    private fun issueToken(
        sessionMode: SessionMode,
        memberId: Long?
    ): String {
        return authSessionIssuer.issue(
            sessionMode = sessionMode,
            memberId = memberId,
            anonymousKeyHash = ANONYMOUS_KEY_HASH
        ).accessToken
    }

    private fun getConsentPolicies(accessToken: String): MockHttpServletResponse {
        return mockMvc.get(CONSENT_POLICIES_PATH) {
            header(AUTHORIZATION, "$BEARER $accessToken")
        }.andReturn().response
    }

    private fun consentPoliciesOf(response: MockHttpServletResponse): List<JsonNode> {
        return jsonMapper.readTree(response.contentAsString).get(CONSENT_POLICIES_FIELD).toList()
    }

    companion object {
        private const val CONSENT_POLICIES_PATH = "/v1/consent-policies"
        private const val CONSENT_POLICIES_FIELD = "consentPolicies"
        private const val BEARER = "Bearer"
        private const val MEMBER_ID = 1L
        private const val ANONYMOUS_KEY_HASH = "anonymous-key-hash"
        private const val TERMS_OF_SERVICE_ID = "terms-of-service"
        private const val PRIVACY_POLICY_ID = "privacy-policy"
        private const val HTTPS_PREFIX = "https://"
        private const val CONSENT_POLICY_COUNT = 2
        private val CONSENT_POLICY_FIELDS = arrayOf("policyId", "version", "title", "url", "required")
    }
}
