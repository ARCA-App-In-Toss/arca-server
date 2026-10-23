package com.arca.terms.dto.response

import com.arca.terms.domain.TermsDocument
import com.arca.terms.domain.TermsDocumentFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class TermsDocumentResponse(
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
        description = "시행일 (KST)",
        example = "2026-10-14"
    )
    val effectiveDate: LocalDate,

    @field:Schema(
        description = "본문 형식",
        example = "MARKDOWN"
    )
    val format: TermsDocumentFormat,

    @field:Schema(
        description = "본문",
        example = "## 제1조 목적\n\n이 약관은 ..."
    )
    val content: String
) {
    companion object {
        fun of(
            termsDocument: TermsDocument,
            content: String
        ): TermsDocumentResponse {
            return TermsDocumentResponse(
                policyId = termsDocument.policyId,
                version = termsDocument.version,
                title = termsDocument.title,
                effectiveDate = termsDocument.effectiveDate,
                format = termsDocument.termsDocumentFormat,
                content = content
            )
        }
    }
}
