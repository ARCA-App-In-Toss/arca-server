package com.arca.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class CreateSessionRequest(
    @field:Schema(
        description = "앱인토스 익명 키",
        example = "synthetic-key"
    )
    @field:NotEmpty
    val anonymousKey: String
)
