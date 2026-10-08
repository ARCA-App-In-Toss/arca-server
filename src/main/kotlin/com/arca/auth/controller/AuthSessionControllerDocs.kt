package com.arca.auth.controller

import com.arca.auth.dto.request.CreateSessionRequest
import com.arca.auth.dto.response.CreateSessionResponse
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

@Tag(name = "Auth API", description = "인증 관련 API")
interface AuthSessionControllerDocs {

    @Operation(
        summary = "세션 교환",
        description = "앱인토스 익명 키를 검증하고 세션을 발급합니다. 승객을 만들지 않습니다.<br>" +
            "🔓 <strong>인증 불필요</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "✅ 세션 발급 성공"),
        ApiResponse(
            responseCode = "400",
            description = "🚨 요청 형식 오류",
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
            description = "🚨 익명 키 검증 실패",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "익명 키 검증 실패",
                            value = "{\"error\" : {\"code\" : \"ANONYMOUS_KEY_INVALID\", \"category\" : \"AUTH\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        )
    )
    fun createSession(
        @Valid @RequestBody request: CreateSessionRequest
    ): ResponseEntity<CreateSessionResponse>
}
