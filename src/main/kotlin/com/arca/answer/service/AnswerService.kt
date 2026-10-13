package com.arca.answer.service

import com.arca.answer.domain.Answer
import com.arca.answer.dto.request.CreateAnswerRequest
import com.arca.answer.dto.request.UpdateAnswerRequest
import com.arca.answer.dto.response.AnswerCountResponse
import com.arca.answer.dto.response.AnswerDetailResponse
import com.arca.answer.dto.response.CreateAnswerResponse
import com.arca.answer.dto.response.DeleteAnswerResponse
import com.arca.answer.dto.response.UpdateAnswerResponse
import com.arca.answer.repository.AnswerRepository
import com.arca.auth.domain.AuthSession
import com.arca.global.exception.domain.ExceptionCode.ANSWER_ALREADY_EXISTS
import com.arca.global.exception.domain.ExceptionCode.ANSWER_NOT_FOUND
import com.arca.global.exception.domain.ExceptionCode.DATE_CHANGED
import com.arca.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND
import com.arca.global.exception.domain.ExceptionCode.SEMA_NOT_FOUND
import com.arca.global.exception.domain.ExceptionCode.SEMA_REPLACED
import com.arca.global.exception.domain.ExceptionRecovery
import com.arca.global.exception.domain.RestApiException
import com.arca.global.infra.KstDateProvider
import com.arca.member.repository.MemberRepository
import com.arca.semaphore.repository.SemaphoreRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class AnswerService(
    private val answerRepository: AnswerRepository,
    private val memberRepository: MemberRepository,
    private val semaphoreRepository: SemaphoreRepository,
    private val kstDateProvider: KstDateProvider,
    private val clock: Clock
) {
    @Transactional
    fun createAnswer(
        authSession: AuthSession,
        request: CreateAnswerRequest
    ): CreateAnswerResponse {
        val memberId = authSession.memberId
            ?: throw RestApiException(MEMBER_NOT_FOUND)

        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw RestApiException(MEMBER_NOT_FOUND)

        val now = clock.instant()
        val today = kstDateProvider.dateOf(now)

        if (request.dailySemaId != today.toString()) {
            throw RestApiException(
                exceptionCode = DATE_CHANGED,
                exceptionRecovery = ExceptionRecovery.refreshToday()
            )
        }

        val semaphore = semaphoreRepository.findByDateKst(today)
            ?: throw RestApiException(SEMA_NOT_FOUND)

        if (!semaphore.hasId(request.semaId)) {
            throw RestApiException(
                exceptionCode = SEMA_REPLACED,
                exceptionRecovery = ExceptionRecovery.refreshToday()
            )
        }

        val question = semaphore.findQuestion(request.questionId)
            ?: throw RestApiException(
                exceptionCode = SEMA_REPLACED,
                exceptionRecovery = ExceptionRecovery.refreshToday()
            )

        if (answerRepository.existsByMemberIdAndCreatedDateKst(memberId, today)) {
            throw RestApiException(ANSWER_ALREADY_EXISTS)
        }

        val answer = answerRepository.save(
            Answer.create(
                member = member,
                semaphore = semaphore,
                question = question,
                content = request.content,
                createdAt = now,
                createdDateKst = today
            )
        )

        val activeAnswerCount = answerRepository.countByMemberId(memberId)

        return CreateAnswerResponse.of(
            answer = AnswerDetailResponse.from(answer),
            activeAnswerCount = AnswerCountResponse.of(activeAnswerCount)
        )
    }

    @Transactional
    fun updateAnswer(
        authSession: AuthSession,
        answerId: Long,
        request: UpdateAnswerRequest
    ): UpdateAnswerResponse {
        val memberId = authSession.memberId
            ?: throw RestApiException(MEMBER_NOT_FOUND)

        val answer = answerRepository.findByIdAndMemberId(answerId, memberId)
            ?: throw RestApiException(ANSWER_NOT_FOUND)

        answer.updateContent(request.content, clock.instant())

        val activeAnswerCount = answerRepository.countByMemberId(memberId)

        return UpdateAnswerResponse.of(
            answer = AnswerDetailResponse.from(answer),
            activeAnswerCount = AnswerCountResponse.of(activeAnswerCount)
        )
    }

    @Transactional
    fun deleteAnswer(
        authSession: AuthSession,
        answerId: Long
    ): DeleteAnswerResponse {
        val memberId = authSession.memberId
            ?: throw RestApiException(MEMBER_NOT_FOUND)

        val answer = answerRepository.findByIdAndMemberId(answerId, memberId)
            ?: throw RestApiException(ANSWER_NOT_FOUND)

        answerRepository.delete(answer)

        val activeAnswerCount = answerRepository.countByMemberId(memberId)

        return DeleteAnswerResponse.of(AnswerCountResponse.of(activeAnswerCount))
    }
}
