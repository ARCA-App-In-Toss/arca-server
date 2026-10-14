package com.arca.analytics.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class ProductEventRequest(
    @field:Schema(
        description = "중복 제거 키 (UUID)",
        example = "66d9e9af-2026-4000-8000-000000000003"
    )
    val eventId: UUID,

    @field:Schema(
        description = "이벤트 스키마 버전. 1 고정",
        example = "1"
    )
    val schemaVersion: Int,

    @field:Schema(
        description = "이벤트 이름. 클라이언트 허용 목록만 받는다",
        example = "answer_save_failed"
    )
    val name: String,

    @field:Schema(
        description = "클라이언트 관찰 시각 (순서용)",
        example = "2026-09-13T14:59:00Z"
    )
    val occurredAt: Instant,

    @field:Schema(
        description = "이름별 허용 속성. 허용 속성은 모두 필수이고, 없으면 {}",
        example = "{\"mode\" : \"CREATE\", \"certainty\" : \"UNKNOWN\", \"reasonCode\" : \"NETWORK_UNCONFIRMED\"}"
    )
    val properties: Map<String, String>
)
