package com.arca.semaphore.dto.response

import com.arca.answer.dto.response.AnswerCountResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class TodaySemaphoreResponse(
    @field:Schema(
        description = "서버 기준 오늘 (KST)",
        example = "2026-10-10"
    )
    val dateKst: LocalDate,

    @field:Schema(description = "오늘의 SEMA (두 질문 모두)")
    val sema: SemaphoreResponse,

    @field:Schema(description = "오늘 답변 상태")
    val answer: TodayAnswerResponse,

    @field:Schema(description = "현재 활성 답변 수")
    val activeAnswerCount: AnswerCountResponse
){
    companion object {
        fun of(
            dateKst: LocalDate,
            sema: SemaphoreResponse,
            answer: TodayAnswerResponse,
            activeAnswerCount: AnswerCountResponse
        ): TodaySemaphoreResponse {
            return TodaySemaphoreResponse(
                dateKst = dateKst,
                sema = sema,
                answer = answer,
                activeAnswerCount = activeAnswerCount
            )
        }
    }
}
