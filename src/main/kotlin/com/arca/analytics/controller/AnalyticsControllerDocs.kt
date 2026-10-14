package com.arca.analytics.controller

import com.arca.analytics.dto.request.CreateEventBatchRequest
import com.arca.auth.domain.AuthSession
import com.arca.global.annotation.Auth
import com.arca.global.exception.dto.response.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Analytics API", description = "제품 분석 이벤트 관련 API")
interface AnalyticsControllerDocs {

    @Operation(
        summary = "제품 이벤트 전송",
        description = "허용된 제품 이벤트를 1~20개 batch로 받습니다. best-effort이며 화면 기능을 막지 않습니다.<br>" +
            "허용 이벤트: onboarding_started, onboarding_skipped, today_sema_viewed(answerState), alternate_question_viewed, " +
            "sema_question_changed(from, to), answer_started(mode), answer_save_failed(mode, certainty, reasonCode), archive_viewed(state).<br>" +
            "이름별 허용 속성은 모두 필수이고 다른 속성은 받지 않습니다. 서버 소유 이벤트(onboarding_completed, answer_saved, " +
            "answer_edited, answer_deleted, all_data_deleted)는 거절합니다.<br>" +
            "하나라도 어기면 batch 전체를 거절하고 아무것도 저장하지 않습니다. 같은 eventId는 한 번만 적재합니다.<br>" +
            "분석 주체는 세션에서 정하고, 지표 귀속 기준은 서버 수신 시각입니다.<br>" +
            "🔐 <strong>세션 필요 (GUEST, ACTIVE)</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "202", description = "✅ 이벤트 batch 수락"),
        ApiResponse(
            responseCode = "400",
            description = "🚨 요청 형식 오류, 허용되지 않은 이벤트나 속성 (batch 전체 거절)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "요청 형식 오류",
                            value = "{\"error\" : {\"code\" : \"INVALID_REQUEST\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "401",
            description = "🚨 폐기된 토큰 (모든 데이터 삭제 뒤 등)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "세션 재교환 필요",
                            value = "{\"error\" : {\"code\" : \"SESSION_RECOVERY_REQUIRED\", \"category\" : \"AUTH\", \"requestId\" : \"req-example\", " +
                                "\"recovery\" : {\"kind\" : \"REESTABLISH_SESSION\", \"recoveryAllowed\" : true}}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        )
    )
    fun createEventBatch(
        @Auth authSession: AuthSession,
        @Valid @RequestBody request: CreateEventBatchRequest
    ): ResponseEntity<Void>
}
