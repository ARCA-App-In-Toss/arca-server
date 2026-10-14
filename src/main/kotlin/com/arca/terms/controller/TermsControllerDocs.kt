package com.arca.terms.controller

import com.arca.global.exception.dto.response.ErrorResponse
import com.arca.terms.dto.response.TermsDocumentResponse
import com.arca.terms.dto.response.TermsListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable

@Tag(name = "Terms API", description = "약관 관련 API")
interface TermsControllerDocs {

    @Operation(
        summary = "현재 정책 목록 조회",
        description = "가입 때 동의해야 하는 현재 정책 목록을 반환합니다. 약관 동의 화면과 회원 가입 요청에 씁니다.<br>" +
            "🔐 <strong>세션 필요 (GUEST, ACTIVE)</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "✅ 정책 목록 조회 성공")
    )
    fun getTerms(): ResponseEntity<TermsListResponse>

    @Operation(
        summary = "정책 본문 조회",
        description = "정책 ID와 버전에 해당하는 정책 전문을 반환합니다. 최신 버전으로 바꿔 응답하지 않습니다. 같은 버전의 본문은 바뀌지 않습니다.<br>" +
            "본문은 Markdown이며 제목, 문단, 목록, 굵게, 링크만 씁니다.<br>" +
            "🔐 <strong>세션 필요 (GUEST, ACTIVE)</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "✅ 정책 본문 조회 성공"),
        ApiResponse(
            responseCode = "404",
            description = "🚨 모르는 정책 ID, 또는 그 정책에 없는 버전",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = [
                        ExampleObject(
                            name = "정책 없음",
                            value = "{\"error\" : {\"code\" : \"POLICY_NOT_FOUND\", \"category\" : \"VALIDATION\", \"requestId\" : \"req-example\"}}"
                        )
                    ],
                    schema = Schema(implementation = ErrorResponse::class)
                )
            ]
        )
    )
    fun getTermsDocument(
        @PathVariable policyId: String,
        @PathVariable version: String
    ): ResponseEntity<TermsDocumentResponse>
}
