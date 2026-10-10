package com.arca.semaphore.fixture

import com.arca.question.domain.Question
import com.arca.semaphore.domain.Semaphore
import java.time.LocalDate

object SemaphoreFixture {
    private const val CODE = "TEST-1"

    fun createSemaphore(
        dateKst: LocalDate?,
        primaryQuestion: Question,
        alternateQuestion: Question
    ): Semaphore {
        return createSemaphoreWithDetails(CODE, dateKst, primaryQuestion, alternateQuestion)
    }

    fun createSemaphoreWithDetails(
        code: String,
        dateKst: LocalDate?,
        primaryQuestion: Question,
        alternateQuestion: Question
    ): Semaphore {
        return Semaphore.create(code, dateKst, primaryQuestion, alternateQuestion)
    }
}
