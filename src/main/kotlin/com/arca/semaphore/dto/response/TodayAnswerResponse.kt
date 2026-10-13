package com.arca.semaphore.dto.response

import com.arca.answer.domain.Answer
import com.arca.answer.domain.AnswerState
import com.arca.answer.domain.AnswerState.ANSWERED
import com.arca.answer.domain.AnswerState.UNANSWERED
import com.arca.answer.dto.response.AnswerDetailResponse
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.media.Schema

data class TodayAnswerResponse(
    @field:Schema(
        description = "오늘 배포 단위의 활성 답변 여부",
        example = "ANSWERED"
    )
    val state: AnswerState,

    @field:Schema(
        description = "ANSWERED일 때만 있음. 답변한 질문과 원문 전체",
        nullable = true
    )
    @field:JsonInclude(NON_NULL)
    val value: AnswerDetailResponse?
) {
    companion object {
        fun from(answer: Answer?): TodayAnswerResponse {
            if (answer == null) {
                return TodayAnswerResponse(
                    state = UNANSWERED,
                    value = null
                )
            }

            return TodayAnswerResponse(
                state = ANSWERED,
                value = AnswerDetailResponse.from(answer)
            )
        }
    }
}
