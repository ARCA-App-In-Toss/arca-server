package com.arca.question.dto.response

import com.arca.answer.domain.AnswerState
import io.swagger.v3.oas.annotations.media.Schema

data class TodayAnswerResponse(
    @field:Schema(
        description = "오늘 배포 단위의 활성 답변 여부",
        example = "UNANSWERED"
    )
    val state: AnswerState
) {
    companion object {
        fun of(answerState: AnswerState): TodayAnswerResponse {
            return TodayAnswerResponse(state = answerState)
        }
    }
}
