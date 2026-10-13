package com.arca.semaphore.controller

import com.arca.global.infra.IntegrationTest
import com.arca.global.infra.KstDateProvider
import com.arca.question.domain.Question
import com.arca.question.fixture.QuestionFixture
import com.arca.semaphore.domain.Semaphore
import com.arca.semaphore.fixture.SemaphoreFixture
import com.arca.semaphore.repository.SemaphoreRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

@IntegrationTest
@AutoConfigureMockMvc
class SemaphoreControllerTest(
    private val semaphoreRepository: SemaphoreRepository,
    private val kstDateProvider: KstDateProvider,
    private val entityManager: EntityManager,
    private val mockMvc: MockMvc,
    private val jsonMapper: JsonMapper
) {

    @Nested
    inner class 오늘의_SEMA를_조회하면 {

        @Test
        fun 계약_필드를_정확히_200으로_응답한다() {
            //given
            saveSemaphore(TODAY_CODE, kstDateProvider.today())
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = getTodaySemaphore(activeToken)

            //then
            val body = bodyOf(response)
            assertThat(response.status).isEqualTo(200)
            assertThat(body.propertyNames()).containsExactlyInAnyOrder(*TODAY_SEMA_FIELDS)
            assertThat(body.get("sema").propertyNames()).containsExactlyInAnyOrder(*SEMA_FIELDS)
            assertThat(body.get("sema").get("primaryQuestion").propertyNames()).containsExactlyInAnyOrder(*QUESTION_FIELDS)
            assertThat(body.get("sema").get("alternateQuestion").propertyNames()).containsExactlyInAnyOrder(*QUESTION_FIELDS)
        }

        @Test
        fun 날짜와_배포_ID는_서버_KST_오늘이다() {
            //given
            val today = kstDateProvider.today()
            saveSemaphore(TODAY_CODE, today)
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val body = bodyOf(getTodaySemaphore(activeToken))

            //then
            assertThat(body.get("dateKst").asString()).isEqualTo(today.toString())
            assertThat(body.get("sema").get("dateKst").asString()).isEqualTo(today.toString())
            assertThat(body.get("sema").get("dailySemaId").asString()).isEqualTo(today.toString())
        }

        @Test
        fun 오늘_세마포어의_콘텐츠와_두_질문을_응답한다() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, kstDateProvider.today())
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val sema = bodyOf(getTodaySemaphore(activeToken)).get("sema")

            //then
            assertThat(sema.get("semaId").asString()).isEqualTo(semaphore.id.toString())
            assertThat(sema.get("semaCode").asString()).isEqualTo(TODAY_CODE)
            assertQuestion(sema.get("primaryQuestion"), semaphore.primaryQuestion, PRIMARY_CONTENT)
            assertQuestion(sema.get("alternateQuestion"), semaphore.alternateQuestion, ALTERNATE_CONTENT)
        }

        @Test
        fun 다른_날짜와_예비_세마포어가_있어도_오늘_것만_응답한다() {
            //given
            val today = kstDateProvider.today()
            saveSemaphore(YESTERDAY_CODE, today.minusDays(1))
            saveSemaphore(TOMORROW_CODE, today.plusDays(1))
            saveSemaphore(RESERVE_CODE, null)
            saveSemaphore(TODAY_CODE, today)
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val sema = bodyOf(getTodaySemaphore(activeToken)).get("sema")

            //then
            assertThat(sema.get("semaCode").asString()).isEqualTo(TODAY_CODE)
        }

        @Test
        fun answer는_state_필드_하나로_UNANSWERED다() {
            //given
            saveSemaphore(TODAY_CODE, kstDateProvider.today())
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val answer = bodyOf(getTodaySemaphore(activeToken)).get("answer")

            //then
            assertThat(answer.propertyNames()).containsExactly("state")
            assertThat(answer.get("state").asString()).isEqualTo(UNANSWERED)
        }

        @Test
        fun 오늘_답변이_있으면_answer는_state와_value로_ANSWERED다() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, kstDateProvider.today())
            val activeToken = signUp(ANONYMOUS_KEY)
            postAnswer(activeToken, semaphore, semaphore.alternateQuestion)

            //when
            val body = bodyOf(getTodaySemaphore(activeToken))

            //then
            val answer = body.get("answer")
            val value = answer.get("value")
            assertThat(answer.propertyNames()).containsExactlyInAnyOrder("state", "value")
            assertThat(answer.get("state").asString()).isEqualTo(ANSWERED)
            assertThat(value.propertyNames()).containsExactlyInAnyOrder(*ANSWER_DETAIL_FIELDS)
            assertThat(value.get("isEdited").asBoolean()).isFalse()
            assertThat(value.get("content").asString()).isEqualTo(ANSWER_CONTENT)
            assertQuestion(value.get("question"), semaphore.alternateQuestion, ALTERNATE_CONTENT)
            assertThat(body.get("activeAnswerCount").get("count").asLong()).isEqualTo(1L)
        }

        @Test
        fun activeAnswerCount는_count_필드_하나로_0이다() {
            //given
            saveSemaphore(TODAY_CODE, kstDateProvider.today())
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val activeAnswerCount = bodyOf(getTodaySemaphore(activeToken)).get("activeAnswerCount")

            //then
            assertThat(activeAnswerCount.propertyNames()).containsExactly("count")
            assertThat(activeAnswerCount.get("count").asLong()).isZero()
        }

        @Test
        fun 다른_회원도_같은_SEMA를_받는다() {
            //given
            saveSemaphore(TODAY_CODE, kstDateProvider.today())
            val activeToken = signUp(ANONYMOUS_KEY)
            val otherActiveToken = signUp(OTHER_ANONYMOUS_KEY)

            //when
            val sema = bodyOf(getTodaySemaphore(activeToken)).get("sema")
            val otherSema = bodyOf(getTodaySemaphore(otherActiveToken)).get("sema")

            //then
            assertThat(otherSema).isEqualTo(sema)
        }
    }

    @Nested
    inner class 조회할_수_없으면 {

        @Test
        fun 오늘_세마포어가_없으면_SEMA_NOT_FOUND() {
            //given
            saveSemaphore(YESTERDAY_CODE, kstDateProvider.today().minusDays(1))
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = getTodaySemaphore(activeToken)

            //then
            assertError(response, 503, "SEMA_NOT_FOUND", "MAINTENANCE")
        }

        @Test
        fun GUEST_세션은_SESSION_SCOPE_INSUFFICIENT() {
            //given
            saveSemaphore(TODAY_CODE, kstDateProvider.today())
            val guestToken = exchangeSession(ANONYMOUS_KEY)

            //when
            val response = getTodaySemaphore(guestToken)

            //then
            assertError(response, 403, "SESSION_SCOPE_INSUFFICIENT", "AUTH")
        }

        @Test
        fun 토큰이_없으면_SESSION_INVALID() {
            //given
            saveSemaphore(TODAY_CODE, kstDateProvider.today())

            //when
            val response = mockMvc.get(TODAY_SEMA_PATH).andReturn().response

            //then
            assertError(response, 401, "SESSION_INVALID", "AUTH")
        }
    }

    private fun saveSemaphore(
        code: String,
        dateKst: LocalDate?
    ): Semaphore {
        val primaryQuestion = saveQuestion(PRIMARY_CONTENT)
        val alternateQuestion = saveQuestion(ALTERNATE_CONTENT)
        return semaphoreRepository.save(
            SemaphoreFixture.createSemaphoreWithDetails(code, dateKst, primaryQuestion, alternateQuestion)
        )
    }

    private fun saveQuestion(content: String): Question {
        val question = QuestionFixture.createQuestionWithDetails(content)
        entityManager.persist(question)
        return question
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

    private fun postAnswer(
        accessToken: String,
        semaphore: Semaphore,
        question: Question
    ): MockHttpServletResponse {
        val request = mapOf(
            "dailySemaId" to kstDateProvider.today().toString(),
            "semaId" to semaphore.id.toString(),
            "questionId" to question.id.toString(),
            "content" to ANSWER_CONTENT
        )
        return mockMvc.post(ANSWERS_PATH) {
            header(AUTHORIZATION, "$BEARER $accessToken")
            contentType = APPLICATION_JSON
            content = jsonMapper.writeValueAsString(request)
        }.andReturn().response
    }

    private fun getTodaySemaphore(accessToken: String): MockHttpServletResponse {
        return mockMvc.get(TODAY_SEMA_PATH) {
            header(AUTHORIZATION, "$BEARER $accessToken")
        }.andReturn().response
    }

    private fun accessTokenOf(response: MockHttpServletResponse): String {
        return bodyOf(response).get("accessToken").asString()
    }

    private fun bodyOf(response: MockHttpServletResponse): JsonNode {
        return jsonMapper.readTree(response.contentAsString)
    }

    private fun assertQuestion(
        questionNode: JsonNode,
        question: Question,
        text: String
    ) {
        assertThat(questionNode.propertyNames()).containsExactlyInAnyOrder(*QUESTION_FIELDS)
        assertThat(questionNode.get("questionId").asString()).isEqualTo(question.id.toString())
        assertThat(questionNode.get("text").asString()).isEqualTo(text)
    }

    private fun assertError(
        response: MockHttpServletResponse,
        status: Int,
        code: String,
        category: String
    ) {
        val error = bodyOf(response).get("error")
        assertThat(response.status).isEqualTo(status)
        assertThat(error.get("code").asString()).isEqualTo(code)
        assertThat(error.get("category").asString()).isEqualTo(category)
    }

    companion object {
        private const val SESSIONS_PATH = "/v1/sessions"
        private const val MEMBERS_PATH = "/v1/members"
        private const val ANSWERS_PATH = "/v1/answers"
        private const val TODAY_SEMA_PATH = "/v1/semas/today"
        private const val BEARER = "Bearer"
        private const val ANONYMOUS_KEY = "semaphore-anonymous-key"
        private const val OTHER_ANONYMOUS_KEY = "semaphore-other-anonymous-key"
        private const val TODAY_CODE = "TEST-TODAY"
        private const val YESTERDAY_CODE = "TEST-YESTERDAY"
        private const val TOMORROW_CODE = "TEST-TOMORROW"
        private const val RESERVE_CODE = "TEST-RESERVE"
        private const val PRIMARY_CONTENT = "테스트 기본 질문"
        private const val ALTERNATE_CONTENT = "테스트 대체 질문"
        private const val UNANSWERED = "UNANSWERED"
        private const val ANSWERED = "ANSWERED"
        private const val ANSWER_CONTENT = "오늘의 기록"
        private const val VALID_CONSENTS_BODY = "{\"consents\": [" +
            "{\"policyId\": \"terms-of-service\", \"version\": \"1\", \"agreed\": true}, " +
            "{\"policyId\": \"privacy-policy\", \"version\": \"1\", \"agreed\": true}" +
            "]}"
        private val TODAY_SEMA_FIELDS = arrayOf("dateKst", "sema", "answer", "activeAnswerCount")
        private val SEMA_FIELDS = arrayOf("dailySemaId", "semaId", "semaCode", "dateKst", "primaryQuestion", "alternateQuestion")
        private val QUESTION_FIELDS = arrayOf("questionId", "text")
        private val ANSWER_DETAIL_FIELDS = arrayOf(
            "answerId", "dailySemaId", "semaId", "semaCode", "createdAt", "updatedAt", "createdDateKst", "isEdited", "question", "content"
        )
    }
}
