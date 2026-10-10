package com.arca.member.controller

import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.consent.domain.Consent
import com.arca.consent.repository.ConsentRepository
import com.arca.global.infra.IntegrationTest
import com.arca.member.repository.MemberRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

@IntegrationTest
@AutoConfigureMockMvc
class MemberControllerTest(
    private val memberRepository: MemberRepository,
    private val consentRepository: ConsentRepository,

    private val mockMvc: MockMvc,
    private val jsonMapper: JsonMapper
) {

    @Nested
    inner class 회원_가입에_성공하면 {

        @Test
        fun ACTIVE_세션과_회원_profile을_201로_응답한다() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)

            //when
            val response = postMember(guestToken, VALID_CONSENTS_BODY)

            //then
            val body = jsonMapper.readTree(response.contentAsString)
            val member = body.get("context").get("member")
            assertThat(response.status).isEqualTo(201)
            assertThat(body.propertyNames()).containsExactlyInAnyOrder(*SESSION_RESPONSE_FIELDS)
            assertThat(body.get("context").get("mode").asString()).isEqualTo(ACTIVE.name)
            assertThat(member.get("memberCode").asString()).matches(MEMBER_CODE_PATTERN)
            assertThat(member.get("nickname").isNull).isTrue()
            assertThat(member.get("revision").asString()).isEqualTo(INITIAL_REVISION)
        }

        @Test
        fun 새_ACTIVE_토큰으로_회원_정보를_조회할_수_있다() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)
            val signUpResponse = postMember(guestToken, VALID_CONSENTS_BODY)

            //when
            val response = getMyMember(accessTokenOf(signUpResponse))

            //then
            assertThat(response.status).isEqualTo(200)
            assertThat(jsonMapper.readTree(response.contentAsString).get("memberCode").asString())
                .isEqualTo(memberCodeOf(signUpResponse))
        }

        @Test
        fun 가입에_쓴_GUEST_토큰은_폐기된다() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)
            postMember(guestToken, VALID_CONSENTS_BODY)

            //when
            val response = getMyMember(guestToken)

            //then
            assertError(response, 401, "SESSION_RECOVERY_REQUIRED", "AUTH")
        }

        @Test
        fun 정책마다_동의_기록을_서버_시각으로_남긴다() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)
            val before = Instant.now()

            //when
            postMember(guestToken, VALID_CONSENTS_BODY)

            //then
            val after = Instant.now()
            val consents = consentRepository.findAll()
            assertThat(consents).extracting(Consent::policyId, Consent::policyVersion)
                .containsExactlyInAnyOrder(
                    tuple(TERMS_OF_SERVICE_ID, CURRENT_VERSION),
                    tuple(PRIVACY_POLICY_ID, CURRENT_VERSION)
                )
            assertThat(consents).allSatisfy { assertThat(it.agreedAt).isBetween(before, after) }
        }

        @Test
        fun 같은_정책을_여러_번_내도_정책마다_한_번만_기록한다() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)
            val content = consentsBodyOf(
                consentOf(TERMS_OF_SERVICE_ID, CURRENT_VERSION),
                consentOf(TERMS_OF_SERVICE_ID, CURRENT_VERSION),
                consentOf(PRIVACY_POLICY_ID, CURRENT_VERSION)
            )

            //when
            val response = postMember(guestToken, content)

            //then
            assertThat(response.status).isEqualTo(201)
            assertThat(consentRepository.count()).isEqualTo(CONSENT_POLICY_COUNT)
        }

        @Test
        fun 같은_익명_키로_다시_교환하면_같은_회원의_ACTIVE_세션을_받는다() {
            //given
            val signUpResponse = postMember(exchangeSession(ANONYMOUS_KEY), VALID_CONSENTS_BODY)

            //when
            val response = postSession(ANONYMOUS_KEY)

            //then
            val context = jsonMapper.readTree(response.contentAsString).get("context")
            assertThat(context.get("mode").asString()).isEqualTo(ACTIVE.name)
            assertThat(context.get("member").get("memberCode").asString()).isEqualTo(memberCodeOf(signUpResponse))
        }
    }

    @Nested
    inner class 이미_회원이_있으면_MEMBER_ALREADY_EXISTS {

        @Test
        fun ACTIVE_세션으로_요청하면() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = postMember(activeToken, VALID_CONSENTS_BODY)

            //then
            assertError(response, 409, "MEMBER_ALREADY_EXISTS", "CONFLICT")
            assertThat(memberRepository.count()).isEqualTo(1)
        }

        @Test
        fun 가입한_키의_다른_GUEST_토큰으로_요청하면() {
            //given
            val firstGuestToken = exchangeSession(ANONYMOUS_KEY)
            val secondGuestToken = exchangeSession(ANONYMOUS_KEY)
            postMember(firstGuestToken, VALID_CONSENTS_BODY)

            //when
            val response = postMember(secondGuestToken, VALID_CONSENTS_BODY)

            //then
            assertError(response, 409, "MEMBER_ALREADY_EXISTS", "CONFLICT")
            assertThat(memberRepository.count()).isEqualTo(1)
        }
    }

    @Nested
    inner class 정책_검증에_실패하면_아무것도_만들지_않는다 {

        @Test
        fun 필수_정책이_빠지면_CONSENT_REQUIRED() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)

            //when
            val response = postMember(guestToken, consentsBodyOf(consentOf(TERMS_OF_SERVICE_ID, CURRENT_VERSION)))

            //then
            assertError(response, 422, "CONSENT_REQUIRED", "VALIDATION")
            assertNothingCreated(guestToken)
        }

        @Test
        fun 버전이_다르면_POLICY_VERSION_CHANGED와_최신_정책_목록() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)
            val content = consentsBodyOf(
                consentOf(TERMS_OF_SERVICE_ID, OUTDATED_VERSION),
                consentOf(PRIVACY_POLICY_ID, CURRENT_VERSION)
            )

            //when
            val response = postMember(guestToken, content)

            //then
            val recovery = errorOf(response).get("recovery")
            assertError(response, 422, "POLICY_VERSION_CHANGED", "VALIDATION")
            assertThat(recovery.get("kind").asString()).isEqualTo("REFRESH_POLICIES")
            assertThat(recovery.has("recoveryAllowed")).isFalse()
            assertThat(recovery.get("policies").toList()).extracting<String> { it.get("policyId").asString() }
                .containsExactly(TERMS_OF_SERVICE_ID, PRIVACY_POLICY_ID)
            assertNothingCreated(guestToken)
        }

        @Test
        fun 모르는_정책_ID가_있으면_POLICY_VERSION_CHANGED() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)
            val content = consentsBodyOf(
                consentOf(TERMS_OF_SERVICE_ID, CURRENT_VERSION),
                consentOf(PRIVACY_POLICY_ID, CURRENT_VERSION),
                consentOf(UNKNOWN_POLICY_ID, CURRENT_VERSION)
            )

            //when
            val response = postMember(guestToken, content)

            //then
            assertError(response, 422, "POLICY_VERSION_CHANGED", "VALIDATION")
            assertNothingCreated(guestToken)
        }
    }

    @Nested
    inner class 가입_요청_형식이_잘못되면_INVALID_REQUEST {

        @Test
        fun consents가_빈_배열이면() {
            //when
            val response = postMember(exchangeSession(ANONYMOUS_KEY), "{\"consents\": []}")

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }

        @Test
        fun consents가_없으면() {
            //when
            val response = postMember(exchangeSession(ANONYMOUS_KEY), "{}")

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }

        @Test
        fun agreed가_false면() {
            //given
            val content = consentsBodyOf(
                consentOf(TERMS_OF_SERVICE_ID, CURRENT_VERSION, false),
                consentOf(PRIVACY_POLICY_ID, CURRENT_VERSION)
            )

            //when
            val response = postMember(exchangeSession(ANONYMOUS_KEY), content)

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }

        @Test
        fun agreed가_없으면() {
            //given
            val content = "{\"consents\": [{\"policyId\": \"$TERMS_OF_SERVICE_ID\", \"version\": \"$CURRENT_VERSION\"}]}"

            //when
            val response = postMember(exchangeSession(ANONYMOUS_KEY), content)

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }

        @Test
        fun 추가_필드가_있으면() {
            //given
            val content = "{\"consents\": [${consentOf(TERMS_OF_SERVICE_ID, CURRENT_VERSION)}], \"extra\": 1}"

            //when
            val response = postMember(exchangeSession(ANONYMOUS_KEY), content)

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }
    }

    @Nested
    inner class 회원_정보를_조회하면 {

        @Test
        fun ACTIVE_세션은_회원_profile을_200으로_받는다() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = getMyMember(activeToken)

            //then
            val body = jsonMapper.readTree(response.contentAsString)
            assertThat(response.status).isEqualTo(200)
            assertThat(body.propertyNames()).containsExactlyInAnyOrder(*MEMBER_PROFILE_FIELDS)
            assertThat(body.get("memberCode").asString()).matches(MEMBER_CODE_PATTERN)
            assertThat(body.get("nickname").isNull).isTrue()
            assertThat(body.get("revision").asString()).isEqualTo(INITIAL_REVISION)
        }

        @Test
        fun GUEST_세션은_MEMBER_NOT_FOUND() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)

            //when
            val response = getMyMember(guestToken)

            //then
            assertError(response, 404, "MEMBER_NOT_FOUND", "VALIDATION")
        }
    }

    @Nested
    inner class 닉네임을_바꾸면 {

        @Test
        fun 설정하면_바뀐_profile을_200으로_받고_revision이_오른다() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = putNickname(activeToken, nicknameBodyOf(NICKNAME, INITIAL_REVISION))

            //then
            val body = jsonMapper.readTree(response.contentAsString)
            assertThat(response.status).isEqualTo(200)
            assertThat(body.propertyNames()).containsExactlyInAnyOrder(*MEMBER_PROFILE_FIELDS)
            assertThat(body.get("nickname").asString()).isEqualTo(NICKNAME)
            assertThat(body.get("revision").asString()).isNotEqualTo(INITIAL_REVISION)
            assertThat(profileOf(activeToken).get("nickname").asString()).isEqualTo(NICKNAME)
        }

        @Test
        fun 같은_닉네임으로_다시_설정해도_revision이_오른다() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)
            val firstRevision = revisionOf(putNickname(activeToken, nicknameBodyOf(NICKNAME, INITIAL_REVISION)))

            //when
            val response = putNickname(activeToken, nicknameBodyOf(NICKNAME, firstRevision))

            //then
            assertThat(response.status).isEqualTo(200)
            assertThat(revisionOf(response)).isNotEqualTo(firstRevision)
        }

        @Test
        fun null이면_닉네임을_해제한다() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)
            val revision = revisionOf(putNickname(activeToken, nicknameBodyOf(NICKNAME, INITIAL_REVISION)))

            //when
            val response = putNickname(activeToken, nicknameBodyOf(null, revision))

            //then
            assertThat(response.status).isEqualTo(200)
            assertThat(jsonMapper.readTree(response.contentAsString).get("nickname").isNull).isTrue()
            assertThat(profileOf(activeToken).get("nickname").isNull).isTrue()
        }

        @Test
        fun 앞뒤_공백을_제거해_저장한다() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = putNickname(activeToken, nicknameBodyOf("  $NICKNAME  ", INITIAL_REVISION))

            //then
            assertThat(jsonMapper.readTree(response.contentAsString).get("nickname").asString()).isEqualTo(NICKNAME)
        }

        @ParameterizedTest
        @ValueSource(strings = ["👨‍👩‍👧👨‍👩‍👧", "가나다라마바사아자차카타", "달 빛", "Moon1"])
        fun 규칙을_지키면_통과한다(nickname: String) {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = putNickname(activeToken, nicknameBodyOf(nickname, INITIAL_REVISION))

            //then
            assertThat(response.status).isEqualTo(200)
            assertThat(jsonMapper.readTree(response.contentAsString).get("nickname").asString()).isEqualTo(nickname)
        }

        @ParameterizedTest
        @ValueSource(strings = ["달", "가나다라마바사아자차카타파", "   ", "달\n빛", "달\u0007빛", "달​빛", "ㅤㅤ", "달 빛"])
        fun 규칙을_어기면_NICKNAME_INVALID이고_바뀌지_않는다(nickname: String) {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = putNickname(activeToken, nicknameBodyOf(nickname, INITIAL_REVISION))

            //then
            assertError(response, 422, "NICKNAME_INVALID", "VALIDATION")
            assertProfileUnchanged(activeToken)
        }

        @Test
        fun 저장_길이_상한을_넘으면_NICKNAME_INVALID() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)
            val nickname = "가${COMBINING_MARK.repeat(COMBINING_MARK_COUNT)}나${COMBINING_MARK.repeat(COMBINING_MARK_COUNT)}"

            //when
            val response = putNickname(activeToken, nicknameBodyOf(nickname, INITIAL_REVISION))

            //then
            assertError(response, 422, "NICKNAME_INVALID", "VALIDATION")
            assertProfileUnchanged(activeToken)
        }

        @Test
        fun expectedRevision이_다르면_REVISION_CONFLICT이고_바뀌지_않는다() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = putNickname(activeToken, nicknameBodyOf(NICKNAME, STALE_REVISION))

            //then
            assertError(response, 409, "REVISION_CONFLICT", "CONFLICT")
            assertProfileUnchanged(activeToken)
        }

        @Test
        fun revision과_닉네임이_둘_다_틀리면_REVISION_CONFLICT가_먼저다() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = putNickname(activeToken, nicknameBodyOf(INVALID_NICKNAME, STALE_REVISION))

            //then
            assertError(response, 409, "REVISION_CONFLICT", "CONFLICT")
        }

        @Test
        fun nickname을_생략하면_INVALID_REQUEST() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = putNickname(activeToken, "{\"expectedRevision\": \"$INITIAL_REVISION\"}")

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
            assertProfileUnchanged(activeToken)
        }

        @Test
        fun expectedRevision이_빈_문자열이면_INVALID_REQUEST() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = putNickname(activeToken, nicknameBodyOf(NICKNAME, ""))

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }

        @Test
        fun GUEST_세션은_SESSION_SCOPE_INSUFFICIENT() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)

            //when
            val response = putNickname(guestToken, nicknameBodyOf(NICKNAME, INITIAL_REVISION))

            //then
            assertError(response, 403, "SESSION_SCOPE_INSUFFICIENT", "AUTH")
        }
    }

    private fun postSession(anonymousKey: String): MockHttpServletResponse {
        return mockMvc.post(SESSIONS_PATH) {
            contentType = APPLICATION_JSON
            content = "{\"anonymousKey\": \"$anonymousKey\"}"
        }.andReturn().response
    }

    private fun exchangeSession(anonymousKey: String): String {
        return accessTokenOf(postSession(anonymousKey))
    }

    private fun signUp(anonymousKey: String): String {
        return accessTokenOf(postMember(exchangeSession(anonymousKey), VALID_CONSENTS_BODY))
    }

    private fun postMember(
        accessToken: String,
        content: String
    ): MockHttpServletResponse {
        return mockMvc.post(MEMBERS_PATH) {
            header(AUTHORIZATION, "$BEARER $accessToken")
            contentType = APPLICATION_JSON
            this.content = content
        }.andReturn().response
    }

    private fun getMyMember(accessToken: String): MockHttpServletResponse {
        return mockMvc.get(MY_MEMBER_PATH) {
            header(AUTHORIZATION, "$BEARER $accessToken")
        }.andReturn().response
    }

    private fun putNickname(
        accessToken: String,
        content: String
    ): MockHttpServletResponse {
        return mockMvc.put(MY_NICKNAME_PATH) {
            header(AUTHORIZATION, "$BEARER $accessToken")
            contentType = APPLICATION_JSON
            this.content = content
        }.andReturn().response
    }

    private fun consentOf(
        policyId: String,
        version: String,
        agreed: Boolean = true
    ): String {
        return "{\"policyId\": \"$policyId\", \"version\": \"$version\", \"agreed\": $agreed}"
    }

    private fun consentsBodyOf(vararg consents: String): String {
        return "{\"consents\": [${consents.joinToString(CONSENT_SEPARATOR)}]}"
    }

    private fun nicknameBodyOf(
        nickname: String?,
        expectedRevision: String
    ): String {
        return jsonMapper.writeValueAsString(
            mapOf(
                "nickname" to nickname,
                "expectedRevision" to expectedRevision
            )
        )
    }

    private fun accessTokenOf(response: MockHttpServletResponse): String {
        return jsonMapper.readTree(response.contentAsString).get("accessToken").asString()
    }

    private fun memberCodeOf(response: MockHttpServletResponse): String {
        return jsonMapper.readTree(response.contentAsString).get("context").get("member").get("memberCode").asString()
    }

    private fun revisionOf(response: MockHttpServletResponse): String {
        return jsonMapper.readTree(response.contentAsString).get("revision").asString()
    }

    private fun profileOf(accessToken: String): JsonNode {
        return jsonMapper.readTree(getMyMember(accessToken).contentAsString)
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

    private fun assertNothingCreated(guestToken: String) {
        assertThat(memberRepository.count()).isZero()
        assertThat(consentRepository.count()).isZero()
        assertError(getMyMember(guestToken), 404, "MEMBER_NOT_FOUND", "VALIDATION")
    }

    private fun assertProfileUnchanged(activeToken: String) {
        val profile = profileOf(activeToken)
        assertThat(profile.get("nickname").isNull).isTrue()
        assertThat(profile.get("revision").asString()).isEqualTo(INITIAL_REVISION)
    }

    companion object {
        private const val SESSIONS_PATH = "/v1/sessions"
        private const val MEMBERS_PATH = "/v1/members"
        private const val MY_MEMBER_PATH = "/v1/members/me"
        private const val MY_NICKNAME_PATH = "/v1/members/me/nickname"
        private const val BEARER = "Bearer"
        private const val ANONYMOUS_KEY = "member-anonymous-key"
        private const val TERMS_OF_SERVICE_ID = "terms-of-service"
        private const val PRIVACY_POLICY_ID = "privacy-policy"
        private const val UNKNOWN_POLICY_ID = "unknown-policy"
        private const val CURRENT_VERSION = "1"
        private const val OUTDATED_VERSION = "0"
        private const val CONSENT_POLICY_COUNT = 2L
        private const val CONSENT_SEPARATOR = ", "
        private const val INITIAL_REVISION = "0"
        private const val STALE_REVISION = "999"
        private const val NICKNAME = "달빛"
        private const val INVALID_NICKNAME = "달"
        private const val MEMBER_CODE_PATTERN = "^ARCA-[A-Z0-9]{5}$"
        private const val COMBINING_MARK = "́"
        private const val COMBINING_MARK_COUNT = 150
        private val SESSION_RESPONSE_FIELDS = arrayOf("accessToken", "expiresAt", "context")
        private val MEMBER_PROFILE_FIELDS = arrayOf("memberCode", "nickname", "revision")
        private const val VALID_CONSENTS_BODY = "{\"consents\": [" +
            "{\"policyId\": \"$TERMS_OF_SERVICE_ID\", \"version\": \"$CURRENT_VERSION\", \"agreed\": true}, " +
            "{\"policyId\": \"$PRIVACY_POLICY_ID\", \"version\": \"$CURRENT_VERSION\", \"agreed\": true}" +
            "]}"
    }
}
