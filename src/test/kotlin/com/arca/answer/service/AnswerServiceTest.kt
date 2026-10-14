package com.arca.answer.service

import com.arca.analytics.domain.ProductEventName.ANSWER_DELETED
import com.arca.analytics.domain.ProductEventName.ANSWER_EDITED
import com.arca.analytics.domain.ProductEventName.ANSWER_SAVED
import com.arca.analytics.repository.ProductEventRepository
import com.arca.answer.domain.Answer
import com.arca.answer.dto.request.CreateAnswerRequest
import com.arca.answer.dto.request.UpdateAnswerRequest
import com.arca.answer.fixture.AnswerFixture
import com.arca.answer.repository.AnswerRepository
import com.arca.auth.domain.AuthSession
import com.arca.auth.fixture.AuthSessionFixture
import com.arca.global.exception.domain.ExceptionCode.ANSWER_ALREADY_EXISTS
import com.arca.global.exception.domain.ExceptionCode.ANSWER_CONTENT_INVALID
import com.arca.global.exception.domain.ExceptionCode.ANSWER_NOT_FOUND
import com.arca.global.exception.domain.ExceptionCode.CURSOR_INVALID
import com.arca.global.exception.domain.ExceptionCode.DATE_CHANGED
import com.arca.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND
import com.arca.global.exception.domain.ExceptionCode.SEMA_NOT_FOUND
import com.arca.global.exception.domain.ExceptionCode.SEMA_REPLACED
import com.arca.global.exception.domain.ExceptionRecoveryKind.REFRESH_TODAY
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

@IntegrationTest
class AnswerServiceTest(
    private val answerService: AnswerService,
    private val answerRepository: AnswerRepository,
    private val productEventRepository: ProductEventRepository,
    private val memberRepository: MemberRepository,
    private val semaphoreRepository: SemaphoreRepository,
    private val kstDateProvider: KstDateProvider,
    private val entityManager: EntityManager
) {
    private lateinit var member: Member
    private lateinit var today: LocalDate

    @BeforeEach
    fun setUp() {
        member = memberRepository.save(MemberFixture.createMemberWithDetails(MEMBER_CODE, ANONYMOUS_KEY_HASH))
        today = kstDateProvider.today()
    }

    @Nested
    inner class 답변을_작성할_때 {

        @Test
        fun 오늘_SEMA의_기본_질문에_답하면_저장하고_활성_답변_수를_돌려준다() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)

            //when
            val response = answerService.createAnswer(activeSession(), createRequestOf(semaphore, semaphore.primaryQuestion, CONTENT))

            //then
            val answer = response.answer
            assertThat(answer.dailySemaId).isEqualTo(today.toString())
            assertThat(answer.semaId).isEqualTo(semaphore.id.toString())
            assertThat(answer.semaCode).isEqualTo(TODAY_CODE)
            assertThat(answer.createdDateKst).isEqualTo(today)
            assertThat(answer.question.questionId).isEqualTo(semaphore.primaryQuestion.id.toString())
            assertThat(answer.question.text).isEqualTo(PRIMARY_CONTENT)
            assertThat(answer.content).isEqualTo(CONTENT)
            assertThat(answer.isEdited).isFalse()
            assertThat(answer.updatedAt).isEqualTo(answer.createdAt)
            assertThat(response.activeAnswerCount.count).isEqualTo(1L)
            assertThat(answerRepository.count()).isEqualTo(1L)
        }

        @Test
        fun 대체_질문을_골라도_저장한다() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)

            //when
            val response = answerService.createAnswer(activeSession(), createRequestOf(semaphore, semaphore.alternateQuestion, CONTENT))

            //then
            assertThat(response.answer.question.questionId).isEqualTo(semaphore.alternateQuestion.id.toString())
            assertThat(response.answer.question.text).isEqualTo(ALTERNATE_CONTENT)
        }

        @Test
        fun 본문은_trim하지_않고_그대로_저장한다() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)

            //when
            val response = answerService.createAnswer(activeSession(), createRequestOf(semaphore, semaphore.primaryQuestion, PADDED_CONTENT))

            //then
            assertThat(response.answer.content).isEqualTo(PADDED_CONTENT)
        }

        @Test
        fun 활성_답변_수는_지난_답변까지_센다() {
            //given
            val pastSemaphore = saveSemaphore(YESTERDAY_CODE, today.minusDays(1))
            answerRepository.save(AnswerFixture.createAnswer(member, pastSemaphore, today.minusDays(1)))
            val semaphore = saveSemaphore(TODAY_CODE, today)

            //when
            val response = answerService.createAnswer(activeSession(), createRequestOf(semaphore, semaphore.primaryQuestion, CONTENT))

            //then
            assertThat(response.activeAnswerCount.count).isEqualTo(2L)
        }

        @Test
        fun dailySemaId가_오늘이_아니면_REFRESH_TODAY와_DATE_CHANGED() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)
            val request = CreateAnswerRequest(
                dailySemaId = today.minusDays(1).toString(),
                semaId = semaphore.id.toString(),
                questionId = semaphore.primaryQuestion.id.toString(),
                content = CONTENT
            )

            //when, then
            assertThatThrownBy { answerService.createAnswer(activeSession(), request) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", DATE_CHANGED)
                .extracting { (it as RestApiException).exceptionRecovery?.kind }
                .isEqualTo(REFRESH_TODAY)
            assertThat(answerRepository.count()).isZero()
        }

        @Test
        fun 날짜가_다르면_오늘_SEMA가_없어도_DATE_CHANGED가_먼저다() {
            //given
            val request = CreateAnswerRequest(
                dailySemaId = today.minusDays(1).toString(),
                semaId = UNKNOWN_ID,
                questionId = UNKNOWN_ID,
                content = CONTENT
            )

            //when, then
            assertThatThrownBy { answerService.createAnswer(activeSession(), request) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", DATE_CHANGED)
        }

        @Test
        fun 오늘_SEMA가_없으면_SEMA_NOT_FOUND() {
            //given
            saveSemaphore(RESERVE_CODE, null)
            val request = CreateAnswerRequest(
                dailySemaId = today.toString(),
                semaId = UNKNOWN_ID,
                questionId = UNKNOWN_ID,
                content = CONTENT
            )

            //when, then
            assertThatThrownBy { answerService.createAnswer(activeSession(), request) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", SEMA_NOT_FOUND)
        }

        @Test
        fun semaId가_오늘_SEMA가_아니면_REFRESH_TODAY와_SEMA_REPLACED() {
            //given
            val reserveSemaphore = saveSemaphore(RESERVE_CODE, null)
            val semaphore = saveSemaphore(TODAY_CODE, today)
            val request = CreateAnswerRequest(
                dailySemaId = today.toString(),
                semaId = reserveSemaphore.id.toString(),
                questionId = semaphore.primaryQuestion.id.toString(),
                content = CONTENT
            )

            //when, then
            assertThatThrownBy { answerService.createAnswer(activeSession(), request) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", SEMA_REPLACED)
                .extracting { (it as RestApiException).exceptionRecovery?.kind }
                .isEqualTo(REFRESH_TODAY)
        }

        @Test
        fun 질문이_오늘_SEMA의_질문이_아니면_SEMA_REPLACED() {
            //given
            val reserveSemaphore = saveSemaphore(RESERVE_CODE, null)
            val semaphore = saveSemaphore(TODAY_CODE, today)

            //when, then
            assertThatThrownBy {
                answerService.createAnswer(activeSession(), createRequestOf(semaphore, reserveSemaphore.primaryQuestion, CONTENT))
            }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", SEMA_REPLACED)
                .extracting { (it as RestApiException).exceptionRecovery?.kind }
                .isEqualTo(REFRESH_TODAY)
        }

        @Test
        fun 작성에_성공하면_세션의_익명_키_주체로_answer_saved를_기록한다() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)
            val activeSession = activeSession()

            //when
            val response = answerService.createAnswer(activeSession, createRequestOf(semaphore, semaphore.primaryQuestion, CONTENT))

            //then
            val productEvent = productEventRepository.findAll().single()
            assertThat(productEvent.productEventName).isEqualTo(ANSWER_SAVED)
            assertThat(productEvent.anonymousKeyHash).isEqualTo(activeSession.anonymousKeyHash)
            assertThat(productEvent.occurredAt).isEqualTo(response.answer.createdAt)
            assertThat(productEvent.properties).isEmpty()
        }

        @Test
        fun 작성이_거절되면_answer_saved를_기록하지_않는다() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)
            answerRepository.save(AnswerFixture.createAnswer(member, semaphore, today))

            //when, then
            assertThatThrownBy {
                answerService.createAnswer(activeSession(), createRequestOf(semaphore, semaphore.primaryQuestion, CONTENT))
            }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_ALREADY_EXISTS)
            assertThat(productEventRepository.count()).isZero()
        }

        @Test
        fun 오늘_답변이_이미_있으면_ANSWER_ALREADY_EXISTS() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)
            answerRepository.save(AnswerFixture.createAnswer(member, semaphore, today))

            //when, then
            assertThatThrownBy {
                answerService.createAnswer(activeSession(), createRequestOf(semaphore, semaphore.alternateQuestion, CONTENT))
            }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_ALREADY_EXISTS)
            assertThat(answerRepository.count()).isEqualTo(1L)
        }

        @Test
        fun SEMA가_교체됐으면_답변_중복보다_SEMA_REPLACED가_먼저다() {
            //given
            val reserveSemaphore = saveSemaphore(RESERVE_CODE, null)
            val semaphore = saveSemaphore(TODAY_CODE, today)
            answerRepository.save(AnswerFixture.createAnswer(member, semaphore, today))

            //when, then
            assertThatThrownBy {
                answerService.createAnswer(activeSession(), createRequestOf(reserveSemaphore, reserveSemaphore.primaryQuestion, CONTENT))
            }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", SEMA_REPLACED)
        }

        @Test
        fun 답변이_있으면_본문_규칙보다_ANSWER_ALREADY_EXISTS가_먼저다() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)
            answerRepository.save(AnswerFixture.createAnswer(member, semaphore, today))

            //when, then
            assertThatThrownBy {
                answerService.createAnswer(activeSession(), createRequestOf(semaphore, semaphore.primaryQuestion, EMPTY_CONTENT))
            }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_ALREADY_EXISTS)
        }

        @Test
        fun 오늘_답변을_지운_뒤에는_다시_작성할_수_있다() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)
            val answer = answerRepository.save(AnswerFixture.createAnswer(member, semaphore, today))
            answerService.deleteAnswer(activeSession(), answer.id)

            //when
            val response = answerService.createAnswer(activeSession(), createRequestOf(semaphore, semaphore.primaryQuestion, CONTENT))

            //then
            assertThat(response.answer.content).isEqualTo(CONTENT)
            assertThat(response.activeAnswerCount.count).isEqualTo(1L)
        }

        @Test
        fun GUEST_세션이면_MEMBER_NOT_FOUND() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)

            //when, then
            assertThatThrownBy {
                answerService.createAnswer(AuthSessionFixture.createGuestSession(), createRequestOf(semaphore, semaphore.primaryQuestion, CONTENT))
            }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND)
        }
    }

    @Nested
    inner class 본문_규칙을_검사할_때 {

        @ParameterizedTest
        @ValueSource(strings = [" ", "   ", "\n", "가", "👨‍👩‍👧"])
        fun 공백만_있어도_1자_이상이면_통과한다(content: String) {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)

            //when
            val response = answerService.createAnswer(activeSession(), createRequestOf(semaphore, semaphore.primaryQuestion, content))

            //then
            assertThat(response.answer.content).isEqualTo(content)
        }

        @Test
        fun 빈_문자열이면_ANSWER_CONTENT_INVALID() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)

            //when, then
            assertContentInvalid(semaphore, EMPTY_CONTENT)
        }

        @Test
        fun 확장_grapheme_2000자까지_통과한다() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)
            val content = GRAPHEME.repeat(MAX_GRAPHEME_COUNT)

            //when
            val response = answerService.createAnswer(activeSession(), createRequestOf(semaphore, semaphore.primaryQuestion, content))

            //then
            assertThat(response.answer.content).isEqualTo(content)
        }

        @Test
        fun 확장_grapheme_2001자면_ANSWER_CONTENT_INVALID() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)

            //when, then
            assertContentInvalid(semaphore, GRAPHEME.repeat(MAX_GRAPHEME_COUNT + 1))
        }

        @Test
        fun 코드_포인트_15000개까지_통과한다() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)
            val content = BASE_LETTER + COMBINING_MARK.repeat(MAX_CODE_POINT_COUNT - 1)

            //when
            val response = answerService.createAnswer(activeSession(), createRequestOf(semaphore, semaphore.primaryQuestion, content))

            //then
            assertThat(response.answer.content).isEqualTo(content)
        }

        @Test
        fun grapheme이_적어도_코드_포인트가_15000개를_넘으면_ANSWER_CONTENT_INVALID() {
            //given
            val semaphore = saveSemaphore(TODAY_CODE, today)

            //when, then
            assertContentInvalid(semaphore, BASE_LETTER + COMBINING_MARK.repeat(MAX_CODE_POINT_COUNT))
        }

        private fun assertContentInvalid(
            semaphore: Semaphore,
            content: String
        ) {
            assertThatThrownBy {
                answerService.createAnswer(activeSession(), createRequestOf(semaphore, semaphore.primaryQuestion, content))
            }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_CONTENT_INVALID)
            assertThat(answerRepository.count()).isZero()
        }
    }

    @Nested
    inner class 기록_목록을_조회할_때 {

        @Test
        fun 답변이_없으면_빈_목록과_null_cursor를_돌려준다() {
            //when
            val response = answerService.getAnswers(activeSession(), null)

            //then
            assertThat(response.items).isEmpty()
            assertThat(response.nextCursor).isNull()
        }

        @Test
        fun 스무_개면_한_페이지에_모두_담고_cursor는_null이다() {
            //given
            val answers = saveAnswers(PAGE_SIZE)

            //when
            val response = answerService.getAnswers(activeSession(), null)

            //then
            assertThat(response.items).hasSize(PAGE_SIZE)
            assertThat(response.nextCursor).isNull()
            assertThat(response.items.first().answerId).isEqualTo(answers.last().id.toString())
        }

        @Test
        fun 스물한_개면_두_페이지에_나눠_최신순으로_중복_누락_없이_돌려준다() {
            //given
            val answers = saveAnswers(PAGE_SIZE + 1)

            //when
            val firstPage = answerService.getAnswers(activeSession(), null)
            val secondPage = answerService.getAnswers(activeSession(), firstPage.nextCursor)

            //then
            assertThat(firstPage.items).hasSize(PAGE_SIZE)
            assertThat(firstPage.nextCursor).isEqualTo(firstPage.items.last().answerId)
            assertThat(secondPage.items).hasSize(1)
            assertThat(secondPage.nextCursor).isNull()
            assertThat(firstPage.items + secondPage.items)
                .extracting<String> { it.answerId }
                .containsExactlyElementsOf(answers.reversed().map { it.id.toString() })
        }

        @Test
        fun 항목은_답변한_질문과_SEMA를_담는다() {
            //given
            val answer = saveTodayAnswer()

            //when
            val response = answerService.getAnswers(activeSession(), null)

            //then
            val item = response.items.single()
            assertThat(item.answerId).isEqualTo(answer.id.toString())
            assertThat(item.dailySemaId).isEqualTo(today.toString())
            assertThat(item.semaId).isEqualTo(answer.semaphore.id.toString())
            assertThat(item.semaCode).isEqualTo(TODAY_CODE)
            assertThat(item.createdDateKst).isEqualTo(today)
            assertThat(item.question.questionId).isEqualTo(answer.question.id.toString())
            assertThat(item.question.text).isEqualTo(PRIMARY_CONTENT)
            assertThat(item.isEdited).isFalse()
        }

        @Test
        fun 이어_보는_중에_쓴_답변은_다음_페이지에_끼지_않는다() {
            //given
            val answers = saveAnswers(PAGE_SIZE + 1)
            val firstPage = answerService.getAnswers(activeSession(), null)
            answerRepository.save(AnswerFixture.createAnswer(member, answers.first().semaphore, today.plusDays(1)))

            //when
            val secondPage = answerService.getAnswers(activeSession(), firstPage.nextCursor)

            //then
            assertThat(secondPage.items)
                .extracting<String> { it.answerId }
                .containsExactly(answers.first().id.toString())
        }

        @Test
        fun 마지막으로_본_답변을_지워도_그_cursor로_다음_페이지를_이어_받는다() {
            //given
            val answers = saveAnswers(PAGE_SIZE + 1)
            val firstPage = answerService.getAnswers(activeSession(), null)
            answerService.deleteAnswer(activeSession(), checkNotNull(firstPage.nextCursor).toLong())
            entityManager.flush()

            //when
            val secondPage = answerService.getAnswers(activeSession(), firstPage.nextCursor)

            //then
            assertThat(secondPage.items)
                .extracting<String> { it.answerId }
                .containsExactly(answers.first().id.toString())
        }

        @Test
        fun 이어_보는_중에_수정한_답변은_최신_내용으로_나온다() {
            //given
            val answers = saveAnswers(PAGE_SIZE + 1)
            val firstPage = answerService.getAnswers(activeSession(), null)
            answerService.updateAnswer(activeSession(), answers.first().id, UpdateAnswerRequest(UPDATED_CONTENT))
            entityManager.flush()

            //when
            val secondPage = answerService.getAnswers(activeSession(), firstPage.nextCursor)

            //then
            val item = secondPage.items.single()
            assertThat(item.isEdited).isTrue()
            assertThat(item.updatedAt).isNotEqualTo(item.createdAt)
        }

        @Test
        fun 다른_회원의_답변은_목록에_없다() {
            //given
            val answer = saveTodayAnswer()
            val otherMember = memberRepository.save(MemberFixture.createMemberWithDetails(OTHER_MEMBER_CODE, OTHER_ANONYMOUS_KEY_HASH))
            answerRepository.save(AnswerFixture.createAnswer(otherMember, answer.semaphore, today))

            //when
            val response = answerService.getAnswers(activeSession(), null)

            //then
            assertThat(response.items)
                .extracting<String> { it.answerId }
                .containsExactly(answer.id.toString())
        }

        @Test
        fun 다른_회원_답변의_ID를_cursor로_보내도_자기_답변만_나온다() {
            //given
            val answer = saveTodayAnswer()
            val otherMember = memberRepository.save(MemberFixture.createMemberWithDetails(OTHER_MEMBER_CODE, OTHER_ANONYMOUS_KEY_HASH))
            val otherAnswer = answerRepository.save(AnswerFixture.createAnswer(otherMember, answer.semaphore, today))

            //when
            val response = answerService.getAnswers(activeSession(), otherAnswer.id.toString())

            //then
            assertThat(response.items)
                .extracting<String> { it.answerId }
                .containsExactly(answer.id.toString())
        }

        @ParameterizedTest
        @ValueSource(strings = ["abc", "0", "-1", "", " 1", "1.5"])
        fun 형식이_틀린_cursor면_CURSOR_INVALID(cursor: String) {
            //when, then
            assertThatThrownBy { answerService.getAnswers(activeSession(), cursor) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", CURSOR_INVALID)
        }

        @Test
        fun GUEST_세션이면_MEMBER_NOT_FOUND() {
            //when, then
            assertThatThrownBy { answerService.getAnswers(AuthSessionFixture.createGuestSession(), null) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND)
        }
    }

    @Nested
    inner class 기록_상세를_조회할_때 {

        @Test
        fun 본인_답변을_질문과_원문_그대로_돌려준다() {
            //given
            val answer = saveTodayAnswer()

            //when
            val response = answerService.getAnswer(activeSession(), answer.id)

            //then
            assertThat(response.answerId).isEqualTo(answer.id.toString())
            assertThat(response.semaCode).isEqualTo(TODAY_CODE)
            assertThat(response.question.text).isEqualTo(PRIMARY_CONTENT)
            assertThat(response.content).isEqualTo(TODAY_ANSWER_CONTENT)
        }

        @Test
        fun 없는_답변이면_ANSWER_NOT_FOUND() {
            //when, then
            assertThatThrownBy { answerService.getAnswer(activeSession(), UNKNOWN_ANSWER_ID) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_NOT_FOUND)
        }

        @Test
        fun 지운_답변이면_ANSWER_NOT_FOUND() {
            //given
            val answer = saveTodayAnswer()
            answerService.deleteAnswer(activeSession(), answer.id)

            //when, then
            assertThatThrownBy { answerService.getAnswer(activeSession(), answer.id) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_NOT_FOUND)
        }

        @Test
        fun 다른_회원의_답변이면_ANSWER_NOT_FOUND() {
            //given
            val answer = saveTodayAnswer()
            val otherMember = memberRepository.save(MemberFixture.createMemberWithDetails(OTHER_MEMBER_CODE, OTHER_ANONYMOUS_KEY_HASH))

            //when, then
            assertThatThrownBy { answerService.getAnswer(AuthSessionFixture.createActiveSessionOf(otherMember.id), answer.id) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_NOT_FOUND)
        }

        @Test
        fun GUEST_세션이면_MEMBER_NOT_FOUND() {
            //given
            val answer = saveTodayAnswer()

            //when, then
            assertThatThrownBy { answerService.getAnswer(AuthSessionFixture.createGuestSession(), answer.id) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND)
        }
    }

    @Nested
    inner class 답변을_수정할_때 {

        @Test
        fun 본문을_바꾸고_수정됨으로_표시한다() {
            //given
            val answer = saveTodayAnswer()

            //when
            val response = answerService.updateAnswer(activeSession(), answer.id, UpdateAnswerRequest(UPDATED_CONTENT))

            //then
            assertThat(response.answer.content).isEqualTo(UPDATED_CONTENT)
            assertThat(response.answer.isEdited).isTrue()
            assertThat(response.answer.updatedAt).isAfter(response.answer.createdAt)
            assertThat(response.activeAnswerCount.count).isEqualTo(1L)
        }

        @Test
        fun 질문_SEMA_작성_시각과_날짜는_유지한다() {
            //given
            val answer = saveTodayAnswer()
            val questionId = answer.question.id.toString()
            val semaId = answer.semaphore.id.toString()
            val createdAt = answer.createdAt

            //when
            val response = answerService.updateAnswer(activeSession(), answer.id, UpdateAnswerRequest(UPDATED_CONTENT))

            //then
            assertThat(response.answer.question.questionId).isEqualTo(questionId)
            assertThat(response.answer.semaId).isEqualTo(semaId)
            assertThat(response.answer.createdAt).isEqualTo(createdAt)
            assertThat(response.answer.createdDateKst).isEqualTo(today)
        }

        @Test
        fun 같은_본문으로_다시_수정해도_성공한다() {
            //given
            val answer = saveTodayAnswer()

            //when
            val response = answerService.updateAnswer(activeSession(), answer.id, UpdateAnswerRequest(answer.content))

            //then
            assertThat(response.answer.content).isEqualTo(answer.content)
            assertThat(response.answer.isEdited).isTrue()
        }

        @Test
        fun 수정에_성공하면_수정_시각으로_answer_edited를_기록한다() {
            //given
            val answer = saveTodayAnswer()

            //when
            val response = answerService.updateAnswer(activeSession(), answer.id, UpdateAnswerRequest(UPDATED_CONTENT))

            //then
            val productEvent = productEventRepository.findAll().single()
            assertThat(productEvent.productEventName).isEqualTo(ANSWER_EDITED)
            assertThat(productEvent.anonymousKeyHash).isEqualTo(activeSession().anonymousKeyHash)
            assertThat(productEvent.occurredAt).isEqualTo(response.answer.updatedAt)
        }

        @Test
        fun 같은_본문으로_수정해도_answer_edited를_기록한다() {
            //given
            val answer = saveTodayAnswer()

            //when
            answerService.updateAnswer(activeSession(), answer.id, UpdateAnswerRequest(answer.content))

            //then
            assertThat(productEventRepository.findAll()).extracting<Any> { it.productEventName }
                .containsExactly(ANSWER_EDITED)
        }

        @Test
        fun 수정이_거절되면_answer_edited를_기록하지_않는다() {
            //when, then
            assertThatThrownBy { answerService.updateAnswer(activeSession(), UNKNOWN_ANSWER_ID, UpdateAnswerRequest(UPDATED_CONTENT)) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_NOT_FOUND)
            assertThat(productEventRepository.count()).isZero()
        }

        @Test
        fun 지난_날짜_답변도_수정할_수_있다() {
            //given
            val pastSemaphore = saveSemaphore(YESTERDAY_CODE, today.minusDays(1))
            val answer = answerRepository.save(AnswerFixture.createAnswer(member, pastSemaphore, today.minusDays(1)))

            //when
            val response = answerService.updateAnswer(activeSession(), answer.id, UpdateAnswerRequest(UPDATED_CONTENT))

            //then
            assertThat(response.answer.content).isEqualTo(UPDATED_CONTENT)
            assertThat(response.answer.createdDateKst).isEqualTo(today.minusDays(1))
        }

        @Test
        fun 없는_답변이면_ANSWER_NOT_FOUND() {
            //when, then
            assertThatThrownBy { answerService.updateAnswer(activeSession(), UNKNOWN_ANSWER_ID, UpdateAnswerRequest(UPDATED_CONTENT)) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_NOT_FOUND)
        }

        @Test
        fun 다른_회원의_답변이면_ANSWER_NOT_FOUND이고_바뀌지_않는다() {
            //given
            val answer = saveTodayAnswer()
            val otherMember = memberRepository.save(MemberFixture.createMemberWithDetails(OTHER_MEMBER_CODE, OTHER_ANONYMOUS_KEY_HASH))

            //when, then
            assertThatThrownBy {
                answerService.updateAnswer(AuthSessionFixture.createActiveSessionOf(otherMember.id), answer.id, UpdateAnswerRequest(UPDATED_CONTENT))
            }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_NOT_FOUND)
            assertThat(answer.content).isEqualTo(TODAY_ANSWER_CONTENT)
        }

        @Test
        fun 본문_규칙을_어기면_ANSWER_CONTENT_INVALID이고_바뀌지_않는다() {
            //given
            val answer = saveTodayAnswer()

            //when, then
            assertThatThrownBy { answerService.updateAnswer(activeSession(), answer.id, UpdateAnswerRequest(EMPTY_CONTENT)) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_CONTENT_INVALID)
            assertThat(answer.content).isEqualTo(TODAY_ANSWER_CONTENT)
            assertThat(answer.isEdited()).isFalse()
        }

        @Test
        fun GUEST_세션이면_MEMBER_NOT_FOUND() {
            //given
            val answer = saveTodayAnswer()

            //when, then
            assertThatThrownBy {
                answerService.updateAnswer(AuthSessionFixture.createGuestSession(), answer.id, UpdateAnswerRequest(UPDATED_CONTENT))
            }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND)
        }
    }

    @Nested
    inner class 답변을_삭제할_때 {

        @Test
        fun 답변을_지우고_삭제_후_활성_답변_수를_돌려준다() {
            //given
            val pastSemaphore = saveSemaphore(YESTERDAY_CODE, today.minusDays(1))
            answerRepository.save(AnswerFixture.createAnswer(member, pastSemaphore, today.minusDays(1)))
            val answer = saveTodayAnswer()

            //when
            val response = answerService.deleteAnswer(activeSession(), answer.id)

            //then
            assertThat(response.activeAnswerCount.count).isEqualTo(1L)
            assertThat(answerRepository.findById(answer.id)).isEmpty()
        }

        @Test
        fun 삭제에_성공하면_answer_deleted를_기록한다() {
            //given
            val answer = saveTodayAnswer()

            //when
            answerService.deleteAnswer(activeSession(), answer.id)

            //then
            val productEvent = productEventRepository.findAll().single()
            assertThat(productEvent.productEventName).isEqualTo(ANSWER_DELETED)
            assertThat(productEvent.anonymousKeyHash).isEqualTo(activeSession().anonymousKeyHash)
        }

        @Test
        fun 삭제가_거절되면_answer_deleted를_기록하지_않는다() {
            //when, then
            assertThatThrownBy { answerService.deleteAnswer(activeSession(), UNKNOWN_ANSWER_ID) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_NOT_FOUND)
            assertThat(productEventRepository.count()).isZero()
        }

        @Test
        fun 이미_지운_답변이면_ANSWER_NOT_FOUND() {
            //given
            val answer = saveTodayAnswer()
            answerService.deleteAnswer(activeSession(), answer.id)

            //when, then
            assertThatThrownBy { answerService.deleteAnswer(activeSession(), answer.id) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_NOT_FOUND)
        }

        @Test
        fun 다른_회원의_답변이면_ANSWER_NOT_FOUND이고_지우지_않는다() {
            //given
            val answer = saveTodayAnswer()
            val otherMember = memberRepository.save(MemberFixture.createMemberWithDetails(OTHER_MEMBER_CODE, OTHER_ANONYMOUS_KEY_HASH))

            //when, then
            assertThatThrownBy { answerService.deleteAnswer(AuthSessionFixture.createActiveSessionOf(otherMember.id), answer.id) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", ANSWER_NOT_FOUND)
            assertThat(answerRepository.findById(answer.id)).isPresent()
        }

        @Test
        fun GUEST_세션이면_MEMBER_NOT_FOUND() {
            //given
            val answer = saveTodayAnswer()

            //when, then
            assertThatThrownBy { answerService.deleteAnswer(AuthSessionFixture.createGuestSession(), answer.id) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND)
        }
    }

    private fun activeSession(): AuthSession {
        return AuthSessionFixture.createActiveSessionOf(member.id)
    }

    private fun saveTodayAnswer(): Answer {
        val semaphore = saveSemaphore(TODAY_CODE, today)
        return answerRepository.save(
            AnswerFixture.createAnswerWithDetails(member, semaphore, semaphore.primaryQuestion, TODAY_ANSWER_CONTENT, today)
        )
    }

    private fun saveAnswers(count: Int): List<Answer> {
        val semaphore = saveSemaphore(TODAY_CODE, today)
        return (count - 1 downTo 0).map {
            answerRepository.save(AnswerFixture.createAnswer(member, semaphore, today.minusDays(it.toLong())))
        }
    }

    private fun createRequestOf(
        semaphore: Semaphore,
        question: Question,
        content: String
    ): CreateAnswerRequest {
        return CreateAnswerRequest(
            dailySemaId = today.toString(),
            semaId = semaphore.id.toString(),
            questionId = question.id.toString(),
            content = content
        )
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
        private const val MEMBER_CODE = "ARCA-ANSW1"
        private const val OTHER_MEMBER_CODE = "ARCA-ANSW2"
        private const val ANONYMOUS_KEY_HASH = "answer-anonymous-key-hash"
        private const val OTHER_ANONYMOUS_KEY_HASH = "answer-other-anonymous-key-hash"
        private const val TODAY_CODE = "TEST-TODAY"
        private const val YESTERDAY_CODE = "TEST-YESTERDAY"
        private const val RESERVE_CODE = "TEST-RESERVE"
        private const val PRIMARY_CONTENT = "테스트 기본 질문"
        private const val ALTERNATE_CONTENT = "테스트 대체 질문"
        private const val CONTENT = "오늘의 기록"
        private const val PADDED_CONTENT = "  오늘의 기록\n"
        private const val TODAY_ANSWER_CONTENT = "처음 쓴 기록"
        private const val UPDATED_CONTENT = "고쳐 쓴 기록"
        private const val EMPTY_CONTENT = ""
        private const val UNKNOWN_ID = "999999"
        private const val UNKNOWN_ANSWER_ID = 999999L
        private const val GRAPHEME = "가"
        private const val BASE_LETTER = "a"
        private const val COMBINING_MARK = "́"
        private const val MAX_GRAPHEME_COUNT = 2000
        private const val MAX_CODE_POINT_COUNT = 15000
        private const val PAGE_SIZE = 20
    }
}
