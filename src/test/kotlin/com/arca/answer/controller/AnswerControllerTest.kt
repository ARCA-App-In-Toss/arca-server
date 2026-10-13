package com.arca.answer.controller

import com.arca.global.infra.IntegrationTest
import com.arca.global.infra.KstDateProvider
import com.arca.question.domain.Question
import com.arca.question.fixture.QuestionFixture
import com.arca.semaphore.domain.Semaphore
import com.arca.semaphore.fixture.SemaphoreFixture
import com.arca.semaphore.repository.SemaphoreRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

@IntegrationTest
@AutoConfigureMockMvc
class AnswerControllerTest(
    private val semaphoreRepository: SemaphoreRepository,
    private val kstDateProvider: KstDateProvider,
    private val entityManager: EntityManager,
    private val mockMvc: MockMvc,
    private val jsonMapper: JsonMapper
) {
    private lateinit var semaphore: Semaphore

    @BeforeEach
    fun setUp() {
        semaphore = saveTodaySemaphore()
    }

    @Nested
    inner class 답변을_작성하면 {

        @Test
        fun 계약_필드를_정확히_201로_응답한다() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = postAnswer(activeToken, createBodyOf(semaphore.primaryQuestion, CONTENT))

            //then
            val body = bodyOf(response)
            val answer = body.get("answer")
            assertThat(response.status).isEqualTo(201)
            assertThat(body.propertyNames()).containsExactlyInAnyOrder("answer", "activeAnswerCount")
            assertThat(answer.propertyNames()).containsExactlyInAnyOrder(*ANSWER_DETAIL_FIELDS)
            assertThat(answer.get("question").propertyNames()).containsExactlyInAnyOrder(*QUESTION_FIELDS)
            assertThat(answer.get("answerId").isString).isTrue()
            assertThat(answer.get("isEdited").asBoolean()).isFalse()
            assertThat(answer.get("content").asString()).isEqualTo(CONTENT)
            assertThat(answer.get("createdAt").asString()).endsWith("Z")
            assertThat(body.get("activeAnswerCount").propertyNames()).containsExactly("count")
            assertThat(body.get("activeAnswerCount").get("count").asLong()).isEqualTo(1L)
        }

        @Test
        fun 날짜가_바뀌었으면_422_DATE_CHANGED와_REFRESH_TODAY() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)
            val content = jsonMapper.writeValueAsString(
                mapOf(
                    "dailySemaId" to kstDateProvider.today().minusDays(1).toString(),
                    "semaId" to semaphore.id.toString(),
                    "questionId" to semaphore.primaryQuestion.id.toString(),
                    "content" to CONTENT
                )
            )

            //when
            val response = postAnswer(activeToken, content)

            //then
            assertError(response, 422, "DATE_CHANGED", "VALIDATION")
            assertThat(errorOf(response).get("recovery").get("kind").asString()).isEqualTo("REFRESH_TODAY")
        }

        @Test
        fun 같은_날_두_번째_작성은_409_ANSWER_ALREADY_EXISTS() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)
            postAnswer(activeToken, createBodyOf(semaphore.primaryQuestion, CONTENT))

            //when
            val response = postAnswer(activeToken, createBodyOf(semaphore.alternateQuestion, CONTENT))

            //then
            assertError(response, 409, "ANSWER_ALREADY_EXISTS", "CONFLICT")
        }

        @Test
        fun 질문_원문_같은_추가_필드가_있으면_INVALID_REQUEST() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)
            val content = jsonMapper.writeValueAsString(
                mapOf(
                    "dailySemaId" to kstDateProvider.today().toString(),
                    "semaId" to semaphore.id.toString(),
                    "questionId" to semaphore.primaryQuestion.id.toString(),
                    "content" to CONTENT,
                    "questionText" to PRIMARY_CONTENT
                )
            )

            //when
            val response = postAnswer(activeToken, content)

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }

        @Test
        fun 빈_ID면_INVALID_REQUEST() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)
            val content = jsonMapper.writeValueAsString(
                mapOf(
                    "dailySemaId" to kstDateProvider.today().toString(),
                    "semaId" to "",
                    "questionId" to semaphore.primaryQuestion.id.toString(),
                    "content" to CONTENT
                )
            )

            //when
            val response = postAnswer(activeToken, content)

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }

        @Test
        fun 빈_본문이면_422_ANSWER_CONTENT_INVALID() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = postAnswer(activeToken, createBodyOf(semaphore.primaryQuestion, ""))

            //then
            assertError(response, 422, "ANSWER_CONTENT_INVALID", "VALIDATION")
        }

        @Test
        fun GUEST_세션은_SESSION_SCOPE_INSUFFICIENT() {
            //given
            val guestToken = exchangeSession(ANONYMOUS_KEY)

            //when
            val response = postAnswer(guestToken, createBodyOf(semaphore.primaryQuestion, CONTENT))

            //then
            assertError(response, 403, "SESSION_SCOPE_INSUFFICIENT", "AUTH")
        }
    }

    @Nested
    inner class 답변을_수정하면 {

        @Test
        fun 바뀐_답변을_200으로_받고_수정됨으로_표시된다() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)
            val answerId = answerIdOf(postAnswer(activeToken, createBodyOf(semaphore.primaryQuestion, CONTENT)))

            //when
            val response = putAnswer(activeToken, answerId, updateBodyOf(UPDATED_CONTENT))

            //then
            val body = bodyOf(response)
            assertThat(response.status).isEqualTo(200)
            assertThat(body.propertyNames()).containsExactlyInAnyOrder("answer", "activeAnswerCount")
            assertThat(body.get("answer").get("content").asString()).isEqualTo(UPDATED_CONTENT)
            assertThat(body.get("answer").get("isEdited").asBoolean()).isTrue()
        }

        @Test
        fun expectedRevision을_보내면_추가_필드로_INVALID_REQUEST() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)
            val answerId = answerIdOf(postAnswer(activeToken, createBodyOf(semaphore.primaryQuestion, CONTENT)))
            val content = jsonMapper.writeValueAsString(
                mapOf(
                    "content" to UPDATED_CONTENT,
                    "expectedRevision" to "0"
                )
            )

            //when
            val response = putAnswer(activeToken, answerId, content)

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }

        @Test
        fun 숫자가_아닌_answerId면_INVALID_REQUEST() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)

            //when
            val response = putAnswer(activeToken, NON_NUMERIC_ANSWER_ID, updateBodyOf(UPDATED_CONTENT))

            //then
            assertError(response, 400, "INVALID_REQUEST", "VALIDATION")
        }

        @Test
        fun 다른_회원의_답변이면_404_ANSWER_NOT_FOUND() {
            //given
            val answerId = answerIdOf(postAnswer(signUp(ANONYMOUS_KEY), createBodyOf(semaphore.primaryQuestion, CONTENT)))
            val otherActiveToken = signUp(OTHER_ANONYMOUS_KEY)

            //when
            val response = putAnswer(otherActiveToken, answerId, updateBodyOf(UPDATED_CONTENT))

            //then
            assertError(response, 404, "ANSWER_NOT_FOUND", "VALIDATION")
        }
    }

    @Nested
    inner class 답변을_삭제하면 {

        @Test
        fun 삭제_후_활성_답변_수만_200으로_받는다() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)
            val answerId = answerIdOf(postAnswer(activeToken, createBodyOf(semaphore.primaryQuestion, CONTENT)))

            //when
            val response = deleteAnswer(activeToken, answerId)

            //then
            val body = bodyOf(response)
            assertThat(response.status).isEqualTo(200)
            assertThat(body.propertyNames()).containsExactly("activeAnswerCount")
            assertThat(body.get("activeAnswerCount").get("count").asLong()).isZero()
        }

        @Test
        fun 이미_지운_답변이면_404_ANSWER_NOT_FOUND() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)
            val answerId = answerIdOf(postAnswer(activeToken, createBodyOf(semaphore.primaryQuestion, CONTENT)))
            deleteAnswer(activeToken, answerId)

            //when
            val response = deleteAnswer(activeToken, answerId)

            //then
            assertError(response, 404, "ANSWER_NOT_FOUND", "VALIDATION")
        }

        @Test
        fun 지운_뒤_같은_날_다시_작성할_수_있다() {
            //given
            val activeToken = signUp(ANONYMOUS_KEY)
            val answerId = answerIdOf(postAnswer(activeToken, createBodyOf(semaphore.primaryQuestion, CONTENT)))
            deleteAnswer(activeToken, answerId)

            //when
            val response = postAnswer(activeToken, createBodyOf(semaphore.alternateQuestion, CONTENT))

            //then
            assertThat(response.status).isEqualTo(201)
        }

        @Test
        fun 토큰이_없으면_SESSION_INVALID() {
            //when
            val response = mockMvc.delete("$ANSWERS_PATH/$UNKNOWN_ANSWER_ID").andReturn().response

            //then
            assertError(response, 401, "SESSION_INVALID", "AUTH")
        }
    }

    private fun saveTodaySemaphore(): Semaphore {
        val primaryQuestion = saveQuestion(PRIMARY_CONTENT)
        val alternateQuestion = saveQuestion(ALTERNATE_CONTENT)
        return semaphoreRepository.save(
            SemaphoreFixture.createSemaphore(kstDateProvider.today(), primaryQuestion, alternateQuestion)
        )
    }

    private fun saveQuestion(content: String): Question {
        val question = QuestionFixture.createQuestionWithDetails(content)
        entityManager.persist(question)
        return question
    }

    private fun createBodyOf(
        question: Question,
        content: String
    ): String {
        return jsonMapper.writeValueAsString(
            mapOf(
                "dailySemaId" to kstDateProvider.today().toString(),
                "semaId" to semaphore.id.toString(),
                "questionId" to question.id.toString(),
                "content" to content
            )
        )
    }

    private fun updateBodyOf(content: String): String {
        return jsonMapper.writeValueAsString(mapOf("content" to content))
    }

    private fun exchangeSession(anonymousKey: String): String {
        val response = mockMvc.post(SESSIONS_PATH) {
            contentType = APPLICATION_JSON
            content = "{\"anonymousKey\": \"$anonymousKey\"}"
        }.andReturn().response
        return bodyOf(response).get("accessToken").asString()
    }

    private fun signUp(anonymousKey: String): String {
        val response = mockMvc.post(MEMBERS_PATH) {
            header(AUTHORIZATION, "$BEARER ${exchangeSession(anonymousKey)}")
            contentType = APPLICATION_JSON
            content = VALID_CONSENTS_BODY
        }.andReturn().response
        return bodyOf(response).get("accessToken").asString()
    }

    private fun postAnswer(
        accessToken: String,
        body: String
    ): MockHttpServletResponse {
        return mockMvc.post(ANSWERS_PATH) {
            header(AUTHORIZATION, "$BEARER $accessToken")
            contentType = APPLICATION_JSON
            content = body
        }.andReturn().response
    }

    private fun putAnswer(
        accessToken: String,
        answerId: String,
        body: String
    ): MockHttpServletResponse {
        return mockMvc.put("$ANSWERS_PATH/$answerId") {
            header(AUTHORIZATION, "$BEARER $accessToken")
            contentType = APPLICATION_JSON
            content = body
        }.andReturn().response
    }

    private fun deleteAnswer(
        accessToken: String,
        answerId: String
    ): MockHttpServletResponse {
        return mockMvc.delete("$ANSWERS_PATH/$answerId") {
            header(AUTHORIZATION, "$BEARER $accessToken")
        }.andReturn().response
    }

    private fun answerIdOf(response: MockHttpServletResponse): String {
        return bodyOf(response).get("answer").get("answerId").asString()
    }

    private fun bodyOf(response: MockHttpServletResponse): JsonNode {
        return jsonMapper.readTree(response.contentAsString)
    }

    private fun errorOf(response: MockHttpServletResponse): JsonNode {
        return bodyOf(response).get("error")
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
        private const val MEMBERS_PATH = "/v1/members"
        private const val ANSWERS_PATH = "/v1/answers"
        private const val BEARER = "Bearer"
        private const val ANONYMOUS_KEY = "answer-anonymous-key"
        private const val OTHER_ANONYMOUS_KEY = "answer-other-anonymous-key"
        private const val PRIMARY_CONTENT = "테스트 기본 질문"
        private const val ALTERNATE_CONTENT = "테스트 대체 질문"
        private const val CONTENT = "  오늘의 기록"
        private const val UPDATED_CONTENT = "고쳐 쓴 기록"
        private const val NON_NUMERIC_ANSWER_ID = "abc"
        private const val UNKNOWN_ANSWER_ID = "999999"
        private const val VALID_CONSENTS_BODY = "{\"consents\": [" +
            "{\"policyId\": \"terms-of-service\", \"version\": \"1\", \"agreed\": true}, " +
            "{\"policyId\": \"privacy-policy\", \"version\": \"1\", \"agreed\": true}" +
            "]}"
        private val QUESTION_FIELDS = arrayOf("questionId", "text")
        private val ANSWER_DETAIL_FIELDS = arrayOf(
            "answerId", "dailySemaId", "semaId", "semaCode", "createdAt", "updatedAt", "createdDateKst", "isEdited", "question", "content"
        )
    }
}
