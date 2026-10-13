package com.arca.answer.dto.response

import com.arca.answer.domain.Answer
import com.arca.question.dto.response.QuestionResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate

data class AnswerDetailResponse(
    @field:Schema(
        description = "답변 ID",
        example = "1"
    )
    val answerId: String,

    @field:Schema(
        description = "배포 단위 ID",
        example = "2026-10-11"
    )
    val dailySemaId: String,

    @field:Schema(
        description = "답변한 SEMA 콘텐츠 ID",
        example = "10"
    )
    val semaId: String,

    @field:Schema(
        description = "답변한 SEMA 콘텐츠 코드",
        example = "FAKE-10"
    )
    val semaCode: String,

    @field:Schema(
        description = "생성 시각 (UTC)",
        example = "2026-10-11T05:00:00Z"
    )
    val createdAt: Instant,

    @field:Schema(
        description = "마지막 수정 시각 (UTC)",
        example = "2026-10-11T05:00:00Z"
    )
    val updatedAt: Instant,

    @field:Schema(
        description = "작성 날짜 (KST)",
        example = "2026-10-11"
    )
    val createdDateKst: LocalDate,

    @field:Schema(
        description = "수정 여부",
        example = "false"
    )
    val isEdited: Boolean,

    @field:Schema(description = "답변한 질문")
    val question: QuestionResponse,

    @field:Schema(
        description = "답변 원문 전체",
        example = "  오늘의 기록"
    )
    val content: String
) {
    companion object {
        fun from(answer: Answer): AnswerDetailResponse {
            return AnswerDetailResponse(
                answerId = answer.id.toString(),
                dailySemaId = answer.createdDateKst.toString(),
                semaId = answer.semaphore.id.toString(),
                semaCode = answer.semaphore.code,
                createdAt = answer.createdAt,
                updatedAt = answer.updatedAt,
                createdDateKst = answer.createdDateKst,
                isEdited = answer.isEdited(),
                question = QuestionResponse.from(answer.question),
                content = answer.content
            )
        }
    }
}
