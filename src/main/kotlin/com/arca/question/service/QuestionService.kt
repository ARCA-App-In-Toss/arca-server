package com.arca.question.service

import com.arca.answer.domain.AnswerState
import com.arca.answer.domain.AnswerState.*
import com.arca.answer.dto.response.AnswerCountResponse
import com.arca.global.exception.domain.ExceptionCode.SEMA_NOT_FOUND
import com.arca.global.exception.domain.RestApiException
import com.arca.global.infra.KstDateProvider
import com.arca.question.dto.response.SemaphoreResponse
import com.arca.question.dto.response.TodayAnswerResponse
import com.arca.question.dto.response.TodayQuestionResponse
import com.arca.semaphore.repository.SemaphoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QuestionService(
    private val semaphoreRepository: SemaphoreRepository,

    private val kstDateProvider: KstDateProvider
) {
    @Transactional(readOnly = true)
    fun getTodayQuestion(): TodayQuestionResponse{
        val today = kstDateProvider.today()

        val semaphore = semaphoreRepository.findByDateKst(today)
            ?: throw RestApiException(SEMA_NOT_FOUND)

        return TodayQuestionResponse.of(
            dateKst = today,
            sema = SemaphoreResponse.of(
                semaphore = semaphore,
                dateKst = today
            ),
            answer = TodayAnswerResponse.of(UNANSWERED),
            activeAnswerCount = AnswerCountResponse.of(NO_ACTIVE_ANSWER_COUNT)
        )
    }

    companion object {
        private const val NO_ACTIVE_ANSWER_COUNT = 0L
    }
}