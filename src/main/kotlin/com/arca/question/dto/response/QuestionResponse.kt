package com.arca.question.dto.response

import com.arca.question.domain.Question
import io.swagger.v3.oas.annotations.media.Schema

data class QuestionResponse(
    @field:Schema(
        description = "질문 ID",
        example = "1"
    )
    val questionId: String,

    @field:Schema(
        description = "질문 원문",
        example = "오늘 가장 오래 머문 생각은 무엇이었나요?"
    )
    val text: String
) {
    companion object {
        fun from(question: Question): QuestionResponse {
            return QuestionResponse(
                questionId = question.id.toString(),
                text = question.content
            )
        }
    }
}
