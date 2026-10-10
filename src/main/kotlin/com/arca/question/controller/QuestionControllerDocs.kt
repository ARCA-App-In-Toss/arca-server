package com.arca.question.controller

import com.arca.global.exception.dto.response.ErrorResponse
import com.arca.question.dto.response.TodayQuestionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

@Tag(name = "Question API", description = "오늘의 질문 관련 API")
interface QuestionControllerDocs {

    @Operation(
        summary = "오늘의 질문 조회",
        description = "서버 KST 날짜, 오늘의 SEMA(기본, 대체 질문), 오늘 답변 상태, 활성 답변 수를 한 번에 반환합니다.<br>" +
            "같은 날짜에는 모든 회원이 같은 SEMA를 받습니다. 미응답이면 answer는 state 필드 하나(UNANSWERED)만 가집니다.<br>" +
            "🔐 <strong>세션 필요 (ACTIVE)</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "✅ 오늘의 질문 조회 성공"),
        ApiResponse(
            responseCode = "403",
            description = "🚨 ACTIVE가 아닌 세션",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "세션 mode 부족",
                            value = "{\"error\" : {\"code\" : \"SESSION_SCOPE_INSUFFICIENT\", \"category\" : \"AUTH\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        ),
        ApiResponse(
            responseCode = "503",
            description = "🚨 오늘 날짜에 편성된 SEMA 없음 (운영상 발생하지 않는 것으로 전제)",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "오늘 SEMA 없음",
                            value = "{\"error\" : {\"code\" : \"SEMA_NOT_FOUND\", \"category\" : \"MAINTENANCE\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        )
    )
    fun getTodayQuestion(): ResponseEntity<TodayQuestionResponse>
}
