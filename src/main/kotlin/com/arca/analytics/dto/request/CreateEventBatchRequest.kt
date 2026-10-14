package com.arca.analytics.dto.request

import com.arca.analytics.domain.ProductEventPlatform
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class CreateEventBatchRequest(
    @field:Schema(description = "이벤트 목록 (1~20개)")
    @field:Size(min = 1, max = 20)
    val events: List<ProductEventRequest>,

    @field:Schema(
        description = "앱 버전 (최대 50자)",
        example = "1.0.0"
    )
    @field:Size(max = 50)
    val appVersion: String?,

    @field:Schema(
        description = "플랫폼",
        example = "IOS"
    )
    val platform: ProductEventPlatform?
)
