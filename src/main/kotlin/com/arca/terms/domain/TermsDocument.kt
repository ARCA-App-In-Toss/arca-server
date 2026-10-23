package com.arca.terms.domain

import com.arca.terms.domain.TermsDocumentFormat.MARKDOWN
import java.time.LocalDate

enum class TermsDocument(
    val policyId: String,
    val version: String,
    val title: String,
    val effectiveDate: LocalDate,
    val termsDocumentFormat: TermsDocumentFormat
) {
    TERMS_OF_SERVICE_V1("terms-of-service", "1", "서비스 이용약관", LocalDate.of(2026, 10, 14), MARKDOWN),
    PRIVACY_POLICY_V1("privacy-policy", "1", "개인정보처리방침", LocalDate.of(2026, 10, 14), MARKDOWN);

    companion object {
        fun tryFrom(
            policyId: String,
            version: String
        ): TermsDocument? {
            return entries.firstOrNull { it.policyId == policyId && it.version == version }
        }
    }
}
