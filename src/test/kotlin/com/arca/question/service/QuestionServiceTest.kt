package com.arca.question.service

import com.arca.answer.domain.AnswerState.UNANSWERED
import com.arca.global.exception.domain.ExceptionCode.SEMA_NOT_FOUND
import com.arca.global.exception.domain.RestApiException
import com.arca.global.infra.IntegrationTest
import com.arca.global.infra.KstDateProvider
import com.arca.question.domain.Question
import com.arca.question.domain.QuestionRole.ALTERNATE
import com.arca.question.domain.QuestionRole.PRIMARY
import com.arca.question.fixture.QuestionFixture
import com.arca.semaphore.domain.Semaphore
import com.arca.semaphore.fixture.SemaphoreFixture
import com.arca.semaphore.repository.SemaphoreRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@IntegrationTest
class QuestionServiceTest(
    private val questionService: QuestionService,

    private val semaphoreRepository: SemaphoreRepository,

    private val kstDateProvider: KstDateProvider,
    private val entityManager: EntityManager
) {

    @Nested
    inner class 오늘_세마포어가_있으면 {

        @Test
        fun 서버_KST_오늘_날짜와_오늘_세마포어를_반환한다() {
            //given
            val today = kstDateProvider.today()
            val semaphore = saveSemaphore(TODAY_CODE, today)

            //when
            val response = questionService.getTodayQuestion()

            //then
            assertThat(response.dateKst).isEqualTo(today)
            assertThat(response.sema.dateKst).isEqualTo(today)
            assertThat(response.sema.semaId).isEqualTo(semaphore.id.toString())
            assertThat(response.sema.version).isEqualTo(INITIAL_VERSION)
            assertThat(response.sema.semaCode).isEqualTo(TODAY_CODE)
        }

        @Test
        fun 배포_ID는_오늘_날짜_문자열이다() {
            //given
            val today = kstDateProvider.today()
            saveSemaphore(TODAY_CODE, today)

            //when
            val response = questionService.getTodayQuestion()

            //then
            assertThat(response.sema.dailySemaId).isEqualTo(today.toString())
        }

        @Test
        fun 기본_질문과_대체_질문을_역할과_함께_반환한다() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, kstDateProvider.today())

            //when
            val response = questionService.getTodayQuestion()

            //then
            val primaryQuestion = response.sema.primaryQuestion
            val alternateQuestion = response.sema.alternateQuestion
            assertThat(primaryQuestion.questionId).isEqualTo(semaphore.primaryQuestion.id.toString())
            assertThat(primaryQuestion.version).isEqualTo(INITIAL_VERSION)
            assertThat(primaryQuestion.role).isEqualTo(PRIMARY)
            assertThat(primaryQuestion.text).isEqualTo(PRIMARY_CONTENT)
            assertThat(alternateQuestion.questionId).isEqualTo(semaphore.alternateQuestion.id.toString())
            assertThat(alternateQuestion.version).isEqualTo(INITIAL_VERSION)
            assertThat(alternateQuestion.role).isEqualTo(ALTERNATE)
            assertThat(alternateQuestion.text).isEqualTo(ALTERNATE_CONTENT)
        }

        @Test
        fun 다른_날짜와_예비_세마포어가_있어도_오늘_것만_반환한다() {
            //given
            val today = kstDateProvider.today()
            saveSemaphore(YESTERDAY_CODE, today.minusDays(1))
            saveSemaphore(TOMORROW_CODE, today.plusDays(1))
            saveSemaphore(RESERVE_CODE, null)
            saveSemaphore(TODAY_CODE, today)

            //when
            val response = questionService.getTodayQuestion()

            //then
            assertThat(response.sema.semaCode).isEqualTo(TODAY_CODE)
        }

        @Test
        fun 답변_상태는_미응답이고_활성_답변_수는_0이다() {
            //given
            saveSemaphore(TODAY_CODE, kstDateProvider.today())

            //when
            val response = questionService.getTodayQuestion()

            //then
            assertThat(response.answer.state).isEqualTo(UNANSWERED)
            assertThat(response.activeAnswerCount.count).isZero()
        }
    }

    @Nested
    inner class 오늘_세마포어가_없으면 {

        @Test
        fun 아무_세마포어도_없으면_SEMA_NOT_FOUND() {
            //when & then
            assertThatThrownBy { questionService.getTodayQuestion() }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", SEMA_NOT_FOUND)
        }

        @Test
        fun 다른_날짜와_예비_세마포어만_있으면_SEMA_NOT_FOUND() {
            //given
            val today = kstDateProvider.today()
            saveSemaphore(YESTERDAY_CODE, today.minusDays(1))
            saveSemaphore(TOMORROW_CODE, today.plusDays(1))
            saveSemaphore(RESERVE_CODE, null)

            //when & then
            assertThatThrownBy { questionService.getTodayQuestion() }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", SEMA_NOT_FOUND)
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

    companion object {
        private const val TODAY_CODE = "TEST-TODAY"
        private const val YESTERDAY_CODE = "TEST-YESTERDAY"
        private const val TOMORROW_CODE = "TEST-TOMORROW"
        private const val RESERVE_CODE = "TEST-RESERVE"
        private const val PRIMARY_CONTENT = "테스트 기본 질문"
        private const val ALTERNATE_CONTENT = "테스트 대체 질문"
        private const val INITIAL_VERSION = "1"
    }
}
