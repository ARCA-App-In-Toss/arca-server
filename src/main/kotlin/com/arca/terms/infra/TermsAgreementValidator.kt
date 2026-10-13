package com.arca.terms.infra

import com.arca.global.exception.domain.ExceptionCode.CONSENT_REQUIRED
import com.arca.global.exception.domain.ExceptionCode.POLICY_VERSION_CHANGED
import com.arca.global.exception.domain.ExceptionRecovery
import com.arca.global.exception.domain.RestApiException
import com.arca.terms.domain.Terms
import com.arca.terms.dto.request.TermsAgreementRequest
import com.arca.terms.dto.response.TermsResponse
import org.springframework.stereotype.Component

@Component
class TermsAgreementValidator {

    fun validateTermsAgreement(termsAgreements: List<TermsAgreementRequest>) {
        val hasOutdatedTerms = termsAgreements.any { termsAgreement ->
            Terms.entries.none { it.id == termsAgreement.policyId && it.version == termsAgreement.version }
        }

        if (hasOutdatedTerms) {
            throw RestApiException(
                exceptionCode = POLICY_VERSION_CHANGED,
                exceptionRecovery = ExceptionRecovery.refreshPolicies(
                    Terms.entries.map { TermsResponse.from(it) }
                )
            )
        }

        val hasMissingTermsAgreement = Terms.entries
            .filter { it.required }
            .any { terms -> termsAgreements.none { it.policyId == terms.id } }

        if (hasMissingTermsAgreement) {
            throw RestApiException(CONSENT_REQUIRED)
        }
    }
}