package com.arca.consent.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotEmpty

data class ConsentRequest(
    @field:Schema(
        description = "정책 ID",
        example = "terms-of-service"
    )
    @field:NotEmpty
    val policyId: String,

    @field:Schema(
        description = "정책 버전 (현재 버전과 정확히 일치해야 함)",
        example = "1"
    )
    @field:NotEmpty
    val version: String,

    @field:Schema(
        description = "동의 여부, 항상 true",
        example = "true"
    )
    @field:AssertTrue
    val agreed: Boolean
)