package com.arca.question.fixture

import com.arca.question.domain.Question

object QuestionFixture {
    private const val CONTENT = "테스트 질문"

    fun createQuestion(): Question {
        return createQuestionWithDetails(CONTENT)
    }

    fun createQuestionWithDetails(content: String): Question {
        return Question.create(content)
    }
}
