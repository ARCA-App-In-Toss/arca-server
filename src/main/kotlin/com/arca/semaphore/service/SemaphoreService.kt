package com.arca.semaphore.service

import com.arca.answer.dto.response.AnswerCountResponse
import com.arca.answer.repository.AnswerRepository
import com.arca.auth.domain.AuthSession
import com.arca.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND
import com.arca.global.exception.domain.ExceptionCode.SEMA_NOT_FOUND
import com.arca.global.exception.domain.RestApiException
import com.arca.global.infra.KstDateProvider
import com.arca.semaphore.dto.response.SemaphoreResponse
import com.arca.semaphore.dto.response.TodayAnswerResponse
import com.arca.semaphore.dto.response.TodaySemaphoreResponse
import com.arca.semaphore.repository.SemaphoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SemaphoreService(
    private val semaphoreRepository: SemaphoreRepository,
    private val answerRepository: AnswerRepository,
    private val kstDateProvider: KstDateProvider
) {
    @Transactional(readOnly = true)
    fun getTodaySemaphore(authSession: AuthSession): TodaySemaphoreResponse {
        val memberId = authSession.memberId
            ?: throw RestApiException(MEMBER_NOT_FOUND)

        val today = kstDateProvider.today()

        val semaphore = semaphoreRepository.findByDateKst(today)
            ?: throw RestApiException(SEMA_NOT_FOUND)

        val answer = answerRepository.findByMemberIdAndCreatedDateKst(memberId, today)
        val activeAnswerCount = answerRepository.countByMemberId(memberId)

        return TodaySemaphoreResponse.of(
            dateKst = today,
            sema = SemaphoreResponse.of(
                semaphore = semaphore,
                dateKst = today
            ),
            answer = TodayAnswerResponse.from(answer),
            activeAnswerCount = AnswerCountResponse.of(activeAnswerCount)
        )
    }
}