package com.arca.semaphore.dto.response

import com.arca.question.dto.response.QuestionResponse
import com.arca.semaphore.domain.Semaphore
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class SemaphoreResponse(
    @field:Schema(
        description = "KST 날짜별 배포 ID. 콘텐츠를 교체해도 유지",
        example = "2026-10-10"
    )
    val dailySemaId: String,

    @field:Schema(
        description = "SEMA 콘텐츠 ID. 교체하면 바뀜",
        example = "10"
    )
    val semaId: String,

    @field:Schema(
        description = "SEMA 콘텐츠 코드",
        example = "FAKE-10"
    )
    val semaCode: String,

    @field:Schema(
        description = "배포 날짜 (KST)",
        example = "2026-10-10"
    )
    val dateKst: LocalDate,

    @field:Schema(description = "기본 질문")
    val primaryQuestion: QuestionResponse,

    @field:Schema(description = "대체 질문")
    val alternateQuestion: QuestionResponse
) {
    companion object {
        fun of(
            semaphore: Semaphore,
            dateKst: LocalDate
        ): SemaphoreResponse {
            return SemaphoreResponse(
                dailySemaId = dateKst.toString(),
                semaId = semaphore.id.toString(),
                semaCode = semaphore.code,
                dateKst = dateKst,
                primaryQuestion = QuestionResponse.from(semaphore.primaryQuestion),
                alternateQuestion = QuestionResponse.from(semaphore.alternateQuestion)
            )
        }
    }
}
