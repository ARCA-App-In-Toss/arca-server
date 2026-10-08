package com.arca.global.infra

import com.arca.auth.domain.SessionMode
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.PRE_PASSENGER
import com.arca.auth.infra.AccessTokenHasher
import com.arca.auth.repository.AuthSessionRepository
import com.arca.auth.service.AuthSessionService
import com.arca.global.infra.AuthTestController.Companion.PUBLIC_BODY
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
class AuthInterceptorTest(
    private val authSessionService: AuthSessionService,

    private val authSessionRepository: AuthSessionRepository,

    private val mockMvc: MockMvc,
    private val accessTokenHasher: AccessTokenHasher,
    private val jsonMapper: JsonMapper
) {

    @Nested
    inner class 허용_mode를_선언한_엔드포인트에 {

        @Test
        fun Authorization_헤더가_없으면_SESSION_INVALID() {
            //when
            val response = mockMvc.get(ACTIVE_PATH).andReturn().response

            //then
            assertError(response, 401, "SESSION_INVALID")
        }

        @Test
        fun Bearer_형식이_아니면_SESSION_INVALID() {
            //when
            val response = mockMvc.get(ACTIVE_PATH) {
                header(AUTHORIZATION, "Basic abc")
            }.andReturn().response

            //then
            assertError(response, 401, "SESSION_INVALID")
        }

        @Test
        fun Bearer_뒤에_토큰이_없으면_SESSION_INVALID() {
            //when
            val response = mockMvc.get(ACTIVE_PATH) {
                header(AUTHORIZATION, "$BEARER ")
            }.andReturn().response

            //then
            assertError(response, 401, "SESSION_INVALID")
        }

        @Test
        fun 모르는_토큰이면_SESSION_INVALID() {
            //when
            val response = mockMvc.get(ACTIVE_PATH) {
                header(AUTHORIZATION, "$BEARER $UNKNOWN_TOKEN")
            }.andReturn().response

            //then
            assertError(response, 401, "SESSION_INVALID")
        }

        @Test
        fun 폐기된_토큰이면_SESSION_RECOVERY_REQUIRED와_recovery로_응답한다() {
            //given
            val accessToken = issueToken(ACTIVE, PASSENGER_ID)
            val authSession = checkNotNull(authSessionRepository.findByTokenHash(accessTokenHasher.hash(accessToken)))
            authSessionService.revoke(authSession.id)

            //when
            val response = mockMvc.get(ACTIVE_PATH) {
                header(AUTHORIZATION, "$BEARER $accessToken")
            }.andReturn().response

            //then
            assertError(response, 401, "SESSION_RECOVERY_REQUIRED")
            assertThat(errorOf(response).get("recovery").get("kind").asString()).isEqualTo("REESTABLISH_SESSION")
        }

        @Test
        fun 허용되지_않은_mode면_SESSION_SCOPE_INSUFFICIENT() {
            //given
            val accessToken = issueToken(PRE_PASSENGER, null)

            //when
            val response = mockMvc.get(ACTIVE_PATH) {
                header(AUTHORIZATION, "$BEARER $accessToken")
            }.andReturn().response

            //then
            assertError(response, 403, "SESSION_SCOPE_INSUFFICIENT")
        }

        @Test
        fun 허용된_mode면_세션을_주입한다() {
            //given
            val accessToken = issueToken(ACTIVE, PASSENGER_ID)

            //when
            val response = mockMvc.get(ACTIVE_PATH) {
                header(AUTHORIZATION, "$BEARER $accessToken")
            }.andReturn().response

            //then
            assertThat(response.status).isEqualTo(200)
            assertThat(response.contentAsString).isEqualTo(ACTIVE.name)
        }

        @Test
        fun Bearer는_대소문자를_가리지_않는다() {
            //given
            val accessToken = issueToken(ACTIVE, PASSENGER_ID)

            //when
            val response = mockMvc.get(ACTIVE_PATH) {
                header(AUTHORIZATION, "bearer $accessToken")
            }.andReturn().response

            //then
            assertThat(response.status).isEqualTo(200)
        }
    }

    @Nested
    inner class 허용_mode를_선언하지_않은_엔드포인트는 {

        @Test
        fun 인증_없이_통과한다() {
            //when
            val response = mockMvc.get(PUBLIC_PATH).andReturn().response

            //then
            assertThat(response.status).isEqualTo(200)
            assertThat(response.contentAsString).isEqualTo(PUBLIC_BODY)
        }
    }

    private fun issueToken(
        sessionMode: SessionMode,
        passengerId: Long?
    ): String {
        return authSessionService.issue(
            sessionMode = sessionMode,
            passengerId = passengerId
        ).accessToken
    }

    private fun errorOf(response: MockHttpServletResponse): JsonNode {
        return jsonMapper.readTree(response.contentAsString).get("error")
    }

    private fun assertError(
        response: MockHttpServletResponse,
        status: Int,
        code: String
    ) {
        assertThat(response.status).isEqualTo(status)
        assertThat(errorOf(response).get("code").asString()).isEqualTo(code)
    }

    companion object {
        private const val ACTIVE_PATH = "/v1/test-auth/active"
        private const val PUBLIC_PATH = "/v1/test-auth/public"
        private const val BEARER = "Bearer"
        private const val UNKNOWN_TOKEN = "unknown-token"
        private const val PASSENGER_ID = 1L
    }
}
