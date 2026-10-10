package com.arca.question.dto.response

import com.arca.question.domain.Question
import com.arca.question.domain.QuestionRole
import io.swagger.v3.oas.annotations.media.Schema

data class QuestionSnapshotResponse(
    @field:Schema(
        description = "질문 ID",
        example = "1"
    )
    val questionId: String,

    @field:Schema(
        description = "질문 버전",
        example = "1"
    )
    val version: String,

    @field:Schema(
        description = "기본, 대체 질문 구분",
        example = "PRIMARY"
    )
    val role: QuestionRole,

    @field:Schema(
        description = "질문 원문",
        example = "오늘 가장 오래 머문 생각은 무엇이었나요?"
    )
    val text: String
){
    companion object {
        fun of(
            question: Question,
            questionRole: QuestionRole
        ): QuestionSnapshotResponse {
            return QuestionSnapshotResponse(
                questionId = question.id.toString(),
                version = question.contentVersion.toString(),
                role = questionRole,
                text = question.content
            )
        }
    }
}
