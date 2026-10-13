package com.arca.answer.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class CreateAnswerRequest(
    @field:Schema(
        description = "오늘 조회의 sema.dailySemaId",
        example = "2026-10-11"
    )
    @field:NotEmpty
    val dailySemaId: String,

    @field:Schema(
        description = "현재 SEMA 콘텐츠 ID",
        example = "10"
    )
    @field:NotEmpty
    val semaId: String,

    @field:Schema(
        description = "선택한 질문 ID (현재 SEMA의 기본 또는 대체 질문)",
        example = "1"
    )
    @field:NotEmpty
    val questionId: String,

    @field:Schema(
        description = "답변 원문. trim하지 않고 그대로 저장한다. 확장 grapheme 기준 1~2,000자",
        example = "  오늘의 기록"
    )
    val content: String
)
