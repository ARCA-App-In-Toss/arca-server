package com.arca.auth.dto.response

import com.arca.auth.dto.internal.IssuedTokenDto
import com.arca.consent.dto.response.ConsentPolicyResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class CreateSessionResponse(
    @field:Schema(
        description = "opaque 액세스 토큰",
        example = "synthetic-token"
    )
    val accessToken: String,

    @field:Schema(
        description = "토큰 만료 시각 (UTC)",
        example = "2026-09-20T15:00:00Z"
    )
    val expiresAt: Instant,

    @field:Schema(description = "세션 context")
    val context: SessionContextResponse,

    @field:Schema(description = "현재 정책 목록")
    val consentPolicies: List<ConsentPolicyResponse>
) {
    companion object {
        fun of(
            issuedTokenDto: IssuedTokenDto,
            context: SessionContextResponse,
            consentPolicies: List<ConsentPolicyResponse>
        ): CreateSessionResponse {
            return CreateSessionResponse(
                accessToken = issuedTokenDto.accessToken,
                expiresAt = issuedTokenDto.expiresAt,
                context = context,
                consentPolicies = consentPolicies
            )
        }
    }
}
