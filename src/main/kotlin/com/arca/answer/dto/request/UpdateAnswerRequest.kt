package com.arca.answer.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateAnswerRequest(
    @field:Schema(
        description = "새 답변 원문. 규칙은 작성과 같다",
        example = "  고쳐 쓴 기록"
    )
    val content: String
)
