package com.arca.answer.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class AnswerPageResponse(
    @field:Schema(description = "기록 목록 (answerId 내림차순, 최대 20개)")
    val items: List<AnswerSummaryResponse>,

    @field:Schema(
        description = "다음 페이지 cursor. 다음 페이지가 없으면 null",
        example = "41",
        nullable = true
    )
    val nextCursor: String?
) {
    companion object {
        fun of(
            items: List<AnswerSummaryResponse>,
            nextCursor: String?
        ): AnswerPageResponse {
            return AnswerPageResponse(
                items = items,
                nextCursor = nextCursor
            )
        }
    }
}
