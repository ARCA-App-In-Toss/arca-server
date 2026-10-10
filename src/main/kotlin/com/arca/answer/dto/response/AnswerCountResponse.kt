package com.arca.answer.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class AnswerCountResponse(
    @field:Schema(
        description = "현재 활성 답변 수. 쓰면 1 오르고 지우면 1 내려간다",
        example = "0"
    )
    val count: Long
) {
    companion object {
        fun of(count: Long): AnswerCountResponse {
            return AnswerCountResponse(count = count)
        }
    }
}
