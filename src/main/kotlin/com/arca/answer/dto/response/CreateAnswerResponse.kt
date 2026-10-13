package com.arca.answer.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class CreateAnswerResponse(
    @field:Schema(description = "작성된 답변")
    val answer: AnswerDetailResponse,

    @field:Schema(description = "작성 후 활성 답변 수")
    val activeAnswerCount: AnswerCountResponse
) {
    companion object {
        fun of(
            answer: AnswerDetailResponse,
            activeAnswerCount: AnswerCountResponse
        ): CreateAnswerResponse {
            return CreateAnswerResponse(
                answer = answer,
                activeAnswerCount = activeAnswerCount
            )
        }
    }
}