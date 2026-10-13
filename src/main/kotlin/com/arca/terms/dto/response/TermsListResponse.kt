package com.arca.terms.dto.response

import com.arca.terms.domain.Terms
import io.swagger.v3.oas.annotations.media.Schema

data class TermsListResponse(
    @field:Schema(description = "현재 정책 목록")
    val consentPolicies: List<TermsResponse>
) {
    companion object {
        fun from(consentPolicies: List<Terms>): TermsListResponse {
            return TermsListResponse(
                consentPolicies = consentPolicies.map { TermsResponse.from(it) }
            )
        }
    }
}
