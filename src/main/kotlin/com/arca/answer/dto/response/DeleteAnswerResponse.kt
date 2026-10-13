package com.arca.answer.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class DeleteAnswerResponse(
    @field:Schema(description = "삭제 후 활성 답변 수")
    val activeAnswerCount: AnswerCountResponse
) {
    companion object {
        fun of(activeAnswerCount: AnswerCountResponse): DeleteAnswerResponse {
            return DeleteAnswerResponse(activeAnswerCount = activeAnswerCount)
        }
    }
}
