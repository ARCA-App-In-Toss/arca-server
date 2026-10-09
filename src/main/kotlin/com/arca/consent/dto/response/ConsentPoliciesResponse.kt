package com.arca.consent.dto.response

import com.arca.consent.domain.ConsentPolicy
import io.swagger.v3.oas.annotations.media.Schema

data class ConsentPoliciesResponse(
    @field:Schema(description = "현재 정책 목록")
    val consentPolicies: List<ConsentPolicyResponse>
) {
    companion object {
        fun from(consentPolicies: List<ConsentPolicy>): ConsentPoliciesResponse {
            return ConsentPoliciesResponse(
                consentPolicies = consentPolicies.map { ConsentPolicyResponse.from(it) }
            )
        }
    }
}
