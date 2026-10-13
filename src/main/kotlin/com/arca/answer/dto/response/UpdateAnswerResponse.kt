package com.arca.answer.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateAnswerResponse(
    @field:Schema(description = "수정된 답변")
    val answer: AnswerDetailResponse,

    @field:Schema(description = "수정 후 활성 답변 수")
    val activeAnswerCount: AnswerCountResponse
) {
    companion object {
        fun of(
            answer: AnswerDetailResponse,
            activeAnswerCount: AnswerCountResponse
        ): UpdateAnswerResponse {
            return UpdateAnswerResponse(
                answer = answer,
                activeAnswerCount = activeAnswerCount
            )
        }
    }
}