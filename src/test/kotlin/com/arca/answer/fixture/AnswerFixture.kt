package com.arca.answer.fixture

import com.arca.answer.domain.Answer
import com.arca.member.domain.Member
import com.arca.question.domain.Question
import com.arca.semaphore.domain.Semaphore
import java.time.Instant
import java.time.LocalDate

object AnswerFixture {
    private const val CONTENT = "테스트 답변"
    private val CREATED_AT = Instant.parse("2026-10-01T00:00:00Z")

    fun createAnswer(
        member: Member,
        semaphore: Semaphore,
        createdDateKst: LocalDate
    ): Answer {
        return createAnswerWithDetails(member, semaphore, semaphore.primaryQuestion, CONTENT, createdDateKst)
    }

    fun createAnswerWithDetails(
        member: Member,
        semaphore: Semaphore,
        question: Question,
        content: String,
        createdDateKst: LocalDate
    ): Answer {
        return Answer.create(member, semaphore, question, content, CREATED_AT, createdDateKst)
    }
}
