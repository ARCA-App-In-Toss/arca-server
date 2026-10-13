package com.arca.semaphore.service

import com.arca.answer.domain.AnswerState.ANSWERED
import com.arca.answer.domain.AnswerState.UNANSWERED
import com.arca.answer.fixture.AnswerFixture
import com.arca.answer.repository.AnswerRepository
import com.arca.auth.domain.AuthSession
import com.arca.auth.fixture.AuthSessionFixture
import com.arca.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND
import com.arca.global.exception.domain.ExceptionCode.SEMA_NOT_FOUND
import com.arca.global.exception.domain.RestApiException
import com.arca.global.infra.IntegrationTest
import com.arca.global.infra.KstDateProvider
import com.arca.member.domain.Member
import com.arca.member.fixture.MemberFixture
import com.arca.member.repository.MemberRepository
import com.arca.question.domain.Question
import com.arca.question.fixture.QuestionFixture
import com.arca.semaphore.domain.Semaphore
import com.arca.semaphore.fixture.SemaphoreFixture
import com.arca.semaphore.repository.SemaphoreRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@IntegrationTest
class SemaphoreServiceTest(
    private val semaphoreService: SemaphoreService,
    private val semaphoreRepository: SemaphoreRepository,
    private val answerRepository: AnswerRepository,
    private val memberRepository: MemberRepository,
    private val kstDateProvider: KstDateProvider,
    private val entityManager: EntityManager
) {
    private lateinit var member: Member

    @BeforeEach
    fun setUp() {
        member = memberRepository.save(MemberFixture.createMemberWithDetails(MEMBER_CODE, ANONYMOUS_KEY_HASH))
    }

    @Nested
    inner class 오늘_세마포어가_있으면 {

        @Test
        fun 서버_KST_오늘_날짜와_오늘_세마포어를_반환한다() {
            //given
            val today = kstDateProvider.today()
            val semaphore = saveSemaphore(TODAY_CODE, today)

            //when
            val response = semaphoreService.getTodaySemaphore(activeSession())

            //then
            assertThat(response.dateKst).isEqualTo(today)
            assertThat(response.sema.dateKst).isEqualTo(today)
            assertThat(response.sema.semaId).isEqualTo(semaphore.id.toString())
            assertThat(response.sema.semaCode).isEqualTo(TODAY_CODE)
        }

        @Test
        fun 배포_ID는_오늘_날짜_문자열이다() {
            //given
            val today = kstDateProvider.today()
            saveSemaphore(TODAY_CODE, today)

            //when
            val response = semaphoreService.getTodaySemaphore(activeSession())

            //then
            assertThat(response.sema.dailySemaId).isEqualTo(today.toString())
        }

        @Test
        fun 기본_질문과_대체_질문을_반환한다() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, kstDateProvider.today())

            //when
            val response = semaphoreService.getTodaySemaphore(activeSession())

            //then
            val primaryQuestion = response.sema.primaryQuestion
            val alternateQuestion = response.sema.alternateQuestion
            assertThat(primaryQuestion.questionId).isEqualTo(semaphore.primaryQuestion.id.toString())
            assertThat(primaryQuestion.text).isEqualTo(PRIMARY_CONTENT)
            assertThat(alternateQuestion.questionId).isEqualTo(semaphore.alternateQuestion.id.toString())
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
            val response = semaphoreService.getTodaySemaphore(activeSession())

            //then
            assertThat(response.sema.semaCode).isEqualTo(TODAY_CODE)
        }

        @Test
        fun 답변이_없으면_미응답이고_활성_답변_수는_0이다() {
            //given
            saveSemaphore(TODAY_CODE, kstDateProvider.today())

            //when
            val response = semaphoreService.getTodaySemaphore(activeSession())

            //then
            assertThat(response.answer.state).isEqualTo(UNANSWERED)
            assertThat(response.answer.value).isNull()
            assertThat(response.activeAnswerCount.count).isZero()
        }
    }

    @Nested
    inner class 오늘_답변이_있으면 {

        @Test
        fun 답변_상태와_답변한_질문과_원문을_반환한다() {
            //given
            val today = kstDateProvider.today()
            val semaphore = saveSemaphore(TODAY_CODE, today)
            val answer = answerRepository.save(
                AnswerFixture.createAnswerWithDetails(member, semaphore, semaphore.alternateQuestion, ANSWER_CONTENT, today)
            )

            //when
            val response = semaphoreService.getTodaySemaphore(activeSession())

            //then
            val value = checkNotNull(response.answer.value)
            assertThat(response.answer.state).isEqualTo(ANSWERED)
            assertThat(value.answerId).isEqualTo(answer.id.toString())
            assertThat(value.semaId).isEqualTo(semaphore.id.toString())
            assertThat(value.question.questionId).isEqualTo(semaphore.alternateQuestion.id.toString())
            assertThat(value.question.text).isEqualTo(ALTERNATE_CONTENT)
            assertThat(value.content).isEqualTo(ANSWER_CONTENT)
        }

        @Test
        fun SEMA가_교체돼도_답변한_SEMA와_질문을_그대로_반환한다() {
            //given
            val today = kstDateProvider.today()
            val replacedSemaphore = saveSemaphore(RESERVE_CODE, null)
            saveSemaphore(TODAY_CODE, today)
            answerRepository.save(AnswerFixture.createAnswer(member, replacedSemaphore, today))

            //when
            val response = semaphoreService.getTodaySemaphore(activeSession())

            //then
            val value = checkNotNull(response.answer.value)
            assertThat(response.sema.semaCode).isEqualTo(TODAY_CODE)
            assertThat(value.semaId).isEqualTo(replacedSemaphore.id.toString())
            assertThat(value.semaCode).isEqualTo(RESERVE_CODE)
            assertThat(value.question.questionId).isEqualTo(replacedSemaphore.primaryQuestion.id.toString())
        }

        @Test
        fun 지난_답변만_있으면_미응답이고_활성_답변_수에는_센다() {
            //given
            val today = kstDateProvider.today()
            val pastSemaphore = saveSemaphore(YESTERDAY_CODE, today.minusDays(1))
            answerRepository.save(AnswerFixture.createAnswer(member, pastSemaphore, today.minusDays(1)))
            saveSemaphore(TODAY_CODE, today)

            //when
            val response = semaphoreService.getTodaySemaphore(activeSession())

            //then
            assertThat(response.answer.state).isEqualTo(UNANSWERED)
            assertThat(response.activeAnswerCount.count).isEqualTo(1L)
        }

        @Test
        fun 다른_회원의_답변은_보지_않는다() {
            //given
            val today = kstDateProvider.today()
            val semaphore = saveSemaphore(TODAY_CODE, today)
            val otherMember = memberRepository.save(MemberFixture.createMemberWithDetails(OTHER_MEMBER_CODE, OTHER_ANONYMOUS_KEY_HASH))
            answerRepository.save(AnswerFixture.createAnswer(otherMember, semaphore, today))

            //when
            val response = semaphoreService.getTodaySemaphore(activeSession())

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
            assertThatThrownBy { semaphoreService.getTodaySemaphore(activeSession()) }
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
            assertThatThrownBy { semaphoreService.getTodaySemaphore(activeSession()) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", SEMA_NOT_FOUND)
        }
    }

    @Nested
    inner class 회원_세션이_아니면 {

        @Test
        fun GUEST_세션이면_MEMBER_NOT_FOUND() {
            //given
            saveSemaphore(TODAY_CODE, kstDateProvider.today())

            //when & then
            assertThatThrownBy { semaphoreService.getTodaySemaphore(AuthSessionFixture.createGuestSession()) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND)
        }
    }

    private fun activeSession(): AuthSession {
        return AuthSessionFixture.createActiveSessionOf(member.id)
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
        private const val MEMBER_CODE = "ARCA-SEMA1"
        private const val OTHER_MEMBER_CODE = "ARCA-SEMA2"
        private const val ANONYMOUS_KEY_HASH = "semaphore-anonymous-key-hash"
        private const val OTHER_ANONYMOUS_KEY_HASH = "semaphore-other-anonymous-key-hash"
        private const val TODAY_CODE = "TEST-TODAY"
        private const val YESTERDAY_CODE = "TEST-YESTERDAY"
        private const val TOMORROW_CODE = "TEST-TOMORROW"
        private const val RESERVE_CODE = "TEST-RESERVE"
        private const val PRIMARY_CONTENT = "테스트 기본 질문"
        private const val ALTERNATE_CONTENT = "테스트 대체 질문"
        private const val ANSWER_CONTENT = "오늘의 기록"
    }
}
