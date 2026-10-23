package com.arca.terms.controller

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
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

@IntegrationTest
@AutoConfigureMockMvc
class TermsControllerTest(
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
            val response = getTerms(accessToken)

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
            val response = getTerms(accessToken)

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
            val response = getTerms(accessToken)

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

    @Nested
    inner class 정책_본문을_조회하면 {

        @Test
        fun GUEST_세션은_요청한_정책_버전의_본문을_200으로_받는다() {
            //given
            val accessToken = issueToken(GUEST, null)

            //when
            val response = getTermsDocument(accessToken, TERMS_OF_SERVICE_ID, CURRENT_VERSION)

            //then
            assertThat(response.status).isEqualTo(200)
            val body = jsonMapper.readTree(response.contentAsString)
            assertThat(body.propertyNames()).containsExactlyInAnyOrder(*POLICY_DOCUMENT_FIELDS)
            assertThat(body.get("policyId").asString()).isEqualTo(TERMS_OF_SERVICE_ID)
            assertThat(body.get("version").asString()).isEqualTo(CURRENT_VERSION)
            assertThat(body.get("title").asString()).isEqualTo(TERMS_OF_SERVICE_TITLE)
            assertThat(body.get("effectiveDate").asString()).isEqualTo(EFFECTIVE_DATE)
            assertThat(body.get("format").asString()).isEqualTo(MARKDOWN)
            assertThat(body.get("content").asString()).isNotBlank()
        }

        @Test
        fun ACTIVE_세션도_조회할_수_있다() {
            //given
            val accessToken = issueToken(ACTIVE, MEMBER_ID)

            //when
            val response = getTermsDocument(accessToken, PRIVACY_POLICY_ID, CURRENT_VERSION)

            //then
            assertThat(response.status).isEqualTo(200)
            assertThat(jsonMapper.readTree(response.contentAsString).get("policyId").asString())
                .isEqualTo(PRIVACY_POLICY_ID)
        }

        @Test
        fun 응답에_no_store_캐시_헤더가_붙는다() {
            //given
            val accessToken = issueToken(GUEST, null)

            //when
            val response = getTermsDocument(accessToken, TERMS_OF_SERVICE_ID, CURRENT_VERSION)

            //then
            assertThat(response.getHeader(CACHE_CONTROL)).isEqualTo(NO_STORE)
        }

        @Test
        fun 현재_정책_목록의_모든_항목은_같은_제목의_본문으로_열린다() {
            //given
            val accessToken = issueToken(GUEST, null)
            val consentPolicies = consentPoliciesOf(getTerms(accessToken))

            //when
            val responses = consentPolicies.map { consentPolicy ->
                consentPolicy to getTermsDocument(
                    accessToken,
                    consentPolicy.get("policyId").asString(),
                    consentPolicy.get("version").asString()
                )
            }

            //then
            assertThat(responses).hasSize(CONSENT_POLICY_COUNT).allSatisfy { (consentPolicy, response) ->
                assertThat(response.status).isEqualTo(200)
                assertThat(jsonMapper.readTree(response.contentAsString).get("title").asString())
                    .isEqualTo(consentPolicy.get("title").asString())
            }
        }

        @Test
        fun 본문은_프론트가_해석하지_않는_표_이미지_HTML을_쓰지_않는다() {
            //given
            val accessToken = issueToken(GUEST, null)

            //when
            val contents = listOf(TERMS_OF_SERVICE_ID, PRIVACY_POLICY_ID).map { policyId ->
                jsonMapper.readTree(getTermsDocument(accessToken, policyId, CURRENT_VERSION).contentAsString)
                    .get("content").asString()
            }

            //then
            assertThat(contents).hasSize(CONSENT_POLICY_COUNT).allSatisfy { content ->
                assertThat(content.lines()).noneMatch { it.trimStart().startsWith(TABLE_ROW_PREFIX) }
                assertThat(content).doesNotContain(IMAGE_PREFIX, HTML_TAG_PREFIX)
            }
        }

        @Test
        fun 모르는_정책_ID면_POLICY_NOT_FOUND() {
            //given
            val accessToken = issueToken(GUEST, null)

            //when
            val response = getTermsDocument(accessToken, UNKNOWN_POLICY_ID, CURRENT_VERSION)

            //then
            assertThat(response.status).isEqualTo(404)
            assertThat(errorCodeOf(response)).isEqualTo("POLICY_NOT_FOUND")
        }

        @Test
        fun 정책에_없는_버전이면_최신_버전으로_바꾸지_않고_POLICY_NOT_FOUND() {
            //given
            val accessToken = issueToken(GUEST, null)

            //when
            val response = getTermsDocument(accessToken, TERMS_OF_SERVICE_ID, UNKNOWN_VERSION)

            //then
            assertThat(response.status).isEqualTo(404)
            assertThat(errorCodeOf(response)).isEqualTo("POLICY_NOT_FOUND")
        }

        @Test
        fun 세션이_없으면_SESSION_INVALID() {
            //when
            val response = mockMvc.get("$CONSENT_POLICIES_PATH/$TERMS_OF_SERVICE_ID/versions/$CURRENT_VERSION")
                .andReturn().response

            //then
            assertThat(response.status).isEqualTo(401)
            assertThat(errorCodeOf(response)).isEqualTo("SESSION_INVALID")
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

    private fun getTerms(accessToken: String): MockHttpServletResponse {
        return mockMvc.get(CONSENT_POLICIES_PATH) {
            header(AUTHORIZATION, "$BEARER $accessToken")
        }.andReturn().response
    }

    private fun getTermsDocument(
        accessToken: String,
        policyId: String,
        version: String
    ): MockHttpServletResponse {
        return mockMvc.get("$CONSENT_POLICIES_PATH/$policyId/versions/$version") {
            header(AUTHORIZATION, "$BEARER $accessToken")
        }.andReturn().response
    }

    private fun errorCodeOf(response: MockHttpServletResponse): String {
        return jsonMapper.readTree(response.contentAsString).get("error").get("code").asString()
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
        private const val TERMS_OF_SERVICE_TITLE = "서비스 이용약관"
        private const val CURRENT_VERSION = "1"
        private const val UNKNOWN_VERSION = "999"
        private const val UNKNOWN_POLICY_ID = "unknown-policy"
        private const val EFFECTIVE_DATE = "2026-10-14"
        private const val MARKDOWN = "MARKDOWN"
        private const val NO_STORE = "no-store"
        private const val TABLE_ROW_PREFIX = "|"
        private const val IMAGE_PREFIX = "!["
        private const val HTML_TAG_PREFIX = "<"
        private val CONSENT_POLICY_FIELDS = arrayOf("policyId", "version", "title", "url", "required")
        private val POLICY_DOCUMENT_FIELDS = arrayOf("policyId", "version", "title", "effectiveDate", "format", "content")
    }
}
