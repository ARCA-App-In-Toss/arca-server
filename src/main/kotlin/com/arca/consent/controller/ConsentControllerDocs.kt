package com.arca.consent.controller

import com.arca.consent.dto.response.ConsentPoliciesResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "Consent API", description = "동의 정책 관련 API")
interface ConsentControllerDocs {

    @Operation(
        summary = "현재 정책 목록 조회",
        description = "가입 때 동의해야 하는 현재 정책 목록을 반환합니다. 약관 동의 화면과 회원 가입 요청에 씁니다.<br>" +
            "🔐 <strong>세션 필요 (GUEST, ACTIVE)</strong><br>"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "✅ 정책 목록 조회 성공")
    )
    fun getConsentPolicies(): ResponseEntity<ConsentPoliciesResponse>
}
