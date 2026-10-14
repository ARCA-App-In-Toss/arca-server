package com.arca.analytics.controller

import com.arca.analytics.domain.ProductEvent
import com.arca.analytics.domain.ProductEventName.ANSWER_SAVE_FAILED
import com.arca.analytics.domain.ProductEventName.ONBOARDING_STARTED
import com.arca.analytics.domain.ProductEventName.TODAY_SEMA_VIEWED
import com.arca.analytics.domain.ProductEventPlatform.IOS
import com.arca.analytics.repository.ProductEventRepository
import com.arca.auth.infra.Sha256Hasher
import com.arca.global.infra.IntegrationTest
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.UUID

@IntegrationTest
@AutoConfigureMockMvc
class AnalyticsControllerTest(
    private val productEventRepository: ProductEventRepository,
    private val sha256Hasher: Sha256Hasher,
    private val mockMvc: MockMvc,
    private val jsonMapper: JsonMapper,
    private val entityManager: EntityManager
) {

    @Nested
    inner class 이벤트_batch를_받으면 {

        @Test
        fun GUEST_세션의_이벤트를_세션의_익명_키_주체로_저장하고_202로_응답한다() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)
            val eventId = UUID.randomUUID().toString()
            val before = Instant.now()

            //when
            val response = postEventBatch(
                guestToken,
                batchOf(eventOf(eventId, ANSWER_SAVE_FAILED.code, ANSWER_SAVE_FAILED_PROPERTIES))
            )

            //then
            val after = Instant.now()
            flushAndClear()
            val productEvent = productEventRepository.findAll().single()
            assertThat(response.status).isEqualTo(202)
            assertThat(response.contentAsString).isEmpty()
            assertThat(productEvent.eventId).isEqualTo(eventId)
            assertThat(productEvent.anonymousKeyHash).isEqualTo(sha256Hasher.hash(ANONYMOUS_KEY))
            assertThat(productEvent.productEventName).isEqualTo(ANSWER_SAVE_FAILED)
            assertThat(productEvent.schemaVersion).isEqualTo(SCHEMA_VERSION)
            assertThat(productEvent.properties).containsExactlyInAnyOrderEntriesOf(
                mapOf("mode" to "CREATE", "certainty" to "UNKNOWN", "reasonCode" to "NETWORK_UNCONFIRMED")
            )
            assertThat(productEvent.occurredAt).isEqualTo(Instant.parse(OCCURRED_AT))
            assertThat(productEvent.receivedAt).isBetween(before, after)
            assertThat(productEvent.appVersion).isEqualTo(APP_VERSION)
            assertThat(productEvent.productEventPlatform).isEqualTo(IOS)
        }

        @Test
        fun ACTIVE_세션의_이벤트도_같은_익명_키_주체로_저장한다() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = postEventBatch(activeToken, batchOf(eventOf(name = TODAY_SEMA_VIEWED.code, properties = TODAY_SEMA_VIEWED_PROPERTIES)))

            //then
            val productEvent = productEventRepository.findAll().single { it.productEventName == TODAY_SEMA_VIEWED }
            assertThat(response.status).isEqualTo(202)
            assertThat(productEvent.anonymousKeyHash).isEqualTo(sha256Hasher.hash(ANONYMOUS_KEY))
        }

        @Test
        fun appVersion과_platform을_생략하면_null로_저장한다() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)

            //when
            val response = postEventBatch(guestToken, "{\"events\": [${eventOf()}]}")

            //then
            val productEvent = productEventRepository.findAll().single()
            assertThat(response.status).isEqualTo(202)
            assertThat(productEvent.appVersion).isNull()
            assertThat(productEvent.productEventPlatform).isNull()
        }

        @Test
        fun 이벤트_20개까지_한_번에_저장한다() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)

            //when
            val response = postEventBatch(guestToken, batchOf(*Array(MAX_EVENT_COUNT) { eventOf() }))

            //then
            assertThat(response.status).isEqualTo(202)
            assertThat(productEventRepository.count()).isEqualTo(MAX_EVENT_COUNT.toLong())
        }

        @Test
        fun 이미_받은_eventId를_다시_보내도_202이고_한_번만_저장한다() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)
            val eventId = UUID.randomUUID().toString()
            postEventBatch(guestToken, batchOf(eventOf(eventId)))
            flushAndClear()

            //when
            val response = postEventBatch(guestToken, batchOf(eventOf(eventId), eventOf()))

            //then
            assertThat(response.status).isEqualTo(202)
            assertThat(productEventRepository.findAll()).extracting<String> { it.eventId }
                .hasSize(2)
                .containsOnlyOnce(eventId)
        }

        @Test
        fun 한_batch_안에_같은_eventId가_여럿이면_처음_것만_저장한다() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)
            val eventId = UUID.randomUUID().toString()

            //when
            val response = postEventBatch(
                guestToken,
                batchOf(
                    eventOf(eventId, TODAY_SEMA_VIEWED.code, TODAY_SEMA_VIEWED_PROPERTIES),
                    eventOf(eventId)
                )
            )

            //then
            val productEvent = productEventRepository.findAll().single()
            assertThat(response.status).isEqualTo(202)
            assertThat(productEvent.productEventName).isEqualTo(TODAY_SEMA_VIEWED)
        }

        @Test
        fun 대문자_eventId도_같은_UUID면_중복으로_본다() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)
            val eventId = UUID.randomUUID().toString()
            postEventBatch(guestToken, batchOf(eventOf(eventId)))
            flushAndClear()

            //when
            val response = postEventBatch(guestToken, batchOf(eventOf(eventId.uppercase())))

            //then
            assertThat(response.status).isEqualTo(202)
            assertThat(productEventRepository.count()).isEqualTo(1L)
        }
    }

    @Nested
    inner class 하나라도_어기면_batch_전체를_INVALID_REQUEST로_거절한다 {

        @Test
        fun 이벤트가_0개() {
            assertRejected("{\"events\": []}")
        }

        @Test
        fun 이벤트가_21개() {
            assertRejected(batchOf(*Array(MAX_EVENT_COUNT + 1) { eventOf() }))
        }

        @Test
        fun 모르는_이벤트_이름이고_오류에_입력값을_반사하지_않는다() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)

            //when
            val response = postEventBatch(guestToken, batchOf(eventOf(), eventOf(name = UNKNOWN_EVENT_NAME)))

            //then
            assertInvalidRequest(response)
            assertThat(response.contentAsString).doesNotContain(UNKNOWN_EVENT_NAME)
            assertThat(productEventRepository.count()).isZero()
        }

        @Test
        fun 서버_소유_이벤트_이름() {
            assertRejected(batchOf(eventOf(), eventOf(name = SERVER_EVENT_NAME)))
        }

        @Test
        fun 허용_속성이_빠짐() {
            assertRejected(batchOf(eventOf(), eventOf(name = TODAY_SEMA_VIEWED.code)))
        }

        @Test
        fun 허용되지_않은_속성이_있음() {
            assertRejected(batchOf(eventOf(), eventOf(properties = "{\"answerLength\": \"10\"}")))
        }

        @Test
        fun 허용되지_않은_속성_값() {
            assertRejected(batchOf(eventOf(), eventOf(name = TODAY_SEMA_VIEWED.code, properties = "{\"answerState\": \"DRAFT\"}")))
        }

        @Test
        fun 속성_값이_문자열이_아님() {
            assertRejected(batchOf(eventOf(), eventOf(name = TODAY_SEMA_VIEWED.code, properties = "{\"answerState\": [\"ANSWERED\"]}")))
        }

        @Test
        fun schemaVersion이_1이_아님() {
            assertRejected(batchOf(eventOf(), eventOf(schemaVersion = UNSUPPORTED_SCHEMA_VERSION)))
        }

        @Test
        fun eventId가_UUID_형식이_아님() {
            assertRejected(batchOf(eventOf(), eventOf(INVALID_EVENT_ID)))
        }

        @Test
        fun 필수_필드가_빠짐() {
            assertRejected(batchOf(eventOf(), "{\"eventId\": \"${UUID.randomUUID()}\", \"schemaVersion\": 1, \"name\": \"${ONBOARDING_STARTED.code}\", \"properties\": {}}"))
        }

        @Test
        fun 이벤트에_추가_필드가_있음() {
            assertRejected(
                batchOf(
                    eventOf(),
                    "{\"eventId\": \"${UUID.randomUUID()}\", \"schemaVersion\": 1, \"name\": \"${ONBOARDING_STARTED.code}\", " +
                        "\"occurredAt\": \"$OCCURRED_AT\", \"properties\": {}, \"anonymousKey\": \"$ANONYMOUS_KEY\"}"
                )
            )
        }

        @Test
        fun 모르는_platform() {
            assertRejected("{\"events\": [${eventOf()}], \"platform\": \"WEB\"}")
        }

        @Test
        fun appVersion이_50자를_넘음() {
            assertRejected("{\"events\": [${eventOf()}], \"appVersion\": \"${"1".repeat(MAX_APP_VERSION_LENGTH + 1)}\"}")
        }

        @Test
        fun appVersion은_50자까지_받는다() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)

            //when
            val response = postEventBatch(guestToken, "{\"events\": [${eventOf()}], \"appVersion\": \"${"1".repeat(MAX_APP_VERSION_LENGTH)}\"}")

            //then
            assertThat(response.status).isEqualTo(202)
        }

        private fun assertRejected(content: String) {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)

            //when
            val response = postEventBatch(guestToken, content)

            //then
            assertInvalidRequest(response)
            assertThat(productEventRepository.count()).isZero()
        }
    }

    @Nested
    inner class 세션을_확인할_때 {

        @Test
        fun 모든_데이터_삭제로_폐기된_토큰이면_SESSION_RECOVERY_REQUIRED() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)
            mockMvc.delete(MY_MEMBER_PATH) {
                header(AUTHORIZATION, "$BEARER $activeToken")
            }

            //when
            val response = postEventBatch(activeToken, batchOf(eventOf()))

            //then
            assertThat(response.status).isEqualTo(401)
            assertThat(errorOf(response).get("code").asString()).isEqualTo("SESSION_RECOVERY_REQUIRED")
        }
    }

    private fun exchangeSession(anonymousKey: String): String {
        val response = mockMvc.post(SESSIONS_PATH) {
            contentType = APPLICATION_JSON
            content = "{\"anonymousKey\": \"$anonymousKey\"}"
        }.andReturn().response

        return accessTokenOf(response)
    }

    private fun signUp(anonymousKey: String): String {
        val response = mockMvc.post(MEMBERS_PATH) {
            header(AUTHORIZATION, "$BEARER ${exchangeSession(anonymousKey)}")
            contentType = APPLICATION_JSON
            content = VALID_CONSENTS_BODY
        }.andReturn().response

        return accessTokenOf(response)
    }

    private fun postEventBatch(
        accessToken: String,
        content: String
    ): MockHttpServletResponse {
        return mockMvc.post(EVENT_BATCHES_PATH) {
            header(AUTHORIZATION, "$BEARER $accessToken")
            contentType = APPLICATION_JSON
            this.content = content
        }.andReturn().response
    }

    private fun eventOf(
        eventId: String = UUID.randomUUID().toString(),
        name: String = ONBOARDING_STARTED.code,
        properties: String = EMPTY_PROPERTIES,
        schemaVersion: Int = SCHEMA_VERSION
    ): String {
        return "{\"eventId\": \"$eventId\", \"schemaVersion\": $schemaVersion, \"name\": \"$name\", " +
            "\"occurredAt\": \"$OCCURRED_AT\", \"properties\": $properties}"
    }

    private fun batchOf(vararg events: String): String {
        return "{\"events\": [${events.joinToString(EVENT_SEPARATOR)}], \"appVersion\": \"$APP_VERSION\", \"platform\": \"${IOS.name}\"}"
    }

    private fun accessTokenOf(response: MockHttpServletResponse): String {
        return jsonMapper.readTree(response.contentAsString).get("accessToken").asString()
    }

    private fun errorOf(response: MockHttpServletResponse): JsonNode {
        return jsonMapper.readTree(response.contentAsString).get("error")
    }

    private fun assertInvalidRequest(response: MockHttpServletResponse) {
        assertThat(response.status).isEqualTo(400)
        assertThat(errorOf(response).get("code").asString()).isEqualTo("INVALID_REQUEST")
        assertThat(errorOf(response).get("category").asString()).isEqualTo("VALIDATION")
    }

    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }

    companion object {
        private const val SESSIONS_PATH = "/v1/sessions"
        private const val MEMBERS_PATH = "/v1/members"
        private const val MY_MEMBER_PATH = "/v1/members/me"
        private const val EVENT_BATCHES_PATH = "/v1/analytics/event-batches"
        private const val BEARER = "Bearer"
        private const val ANONYMOUS_KEY = "analytics-anonymous-key"
        private const val SCHEMA_VERSION = 1
        private const val UNSUPPORTED_SCHEMA_VERSION = 2
        private const val OCCURRED_AT = "2026-09-13T14:59:00Z"
        private const val APP_VERSION = "1.0.0"
        private const val MAX_EVENT_COUNT = 20
        private const val MAX_APP_VERSION_LENGTH = 50
        private const val EMPTY_PROPERTIES = "{}"
        private const val EVENT_SEPARATOR = ", "
        private const val UNKNOWN_EVENT_NAME = "secret_screen_viewed"
        private const val SERVER_EVENT_NAME = "answer_saved"
        private const val INVALID_EVENT_ID = "not-a-uuid"
        private const val TODAY_SEMA_VIEWED_PROPERTIES = "{\"answerState\": \"ANSWERED\"}"
        private const val ANSWER_SAVE_FAILED_PROPERTIES =
            "{\"mode\": \"CREATE\", \"certainty\": \"UNKNOWN\", \"reasonCode\": \"NETWORK_UNCONFIRMED\"}"
        private const val VALID_CONSENTS_BODY = "{\"consents\": [" +
            "{\"policyId\": \"terms-of-service\", \"version\": \"1\", \"agreed\": true}, " +
            "{\"policyId\": \"privacy-policy\", \"version\": \"1\", \"agreed\": true}" +
            "]}"
    }
}
