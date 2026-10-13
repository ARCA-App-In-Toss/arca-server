package com.arca.terms.dto.response

import com.arca.terms.domain.Terms
import io.swagger.v3.oas.annotations.media.Schema

data class TermsResponse(
    @field:Schema(
        description = "정책 ID",
        example = "terms-of-service"
    )
    val policyId: String,

    @field:Schema(
        description = "정책 버전",
        example = "1"
    )
    val version: String,

    @field:Schema(
        description = "표시 제목",
        example = "서비스 이용약관"
    )
    val title: String,

    @field:Schema(
        description = "전문 URL",
        example = "https://arca.invalid/policies/terms-of-service/1"
    )
    val url: String,

    @field:Schema(
        description = "필수 여부",
        example = "true"
    )
    val required: Boolean
){
    companion object {
        fun from(terms: Terms): TermsResponse {
            return TermsResponse(
                policyId = terms.id,
                version = terms.version,
                title = terms.title,
                url = terms.url,
                required = terms.required
            )
        }
    }
}
