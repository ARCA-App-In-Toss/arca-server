package com.arca.consent.infra

import com.arca.consent.domain.ConsentPolicy
import com.arca.consent.dto.request.ConsentRequest
import com.arca.consent.dto.response.ConsentPolicyResponse
import com.arca.global.exception.domain.ExceptionCode.CONSENT_REQUIRED
import com.arca.global.exception.domain.ExceptionCode.POLICY_VERSION_CHANGED
import com.arca.global.exception.domain.ExceptionRecovery
import com.arca.global.exception.domain.RestApiException
import org.springframework.stereotype.Component

@Component
class ConsentValidator {

    fun validateConsent(consents: List<ConsentRequest>) {
        val hasOutdatedConsent = consents.any { consent ->
            ConsentPolicy.entries.none { it.id == consent.policyId && it.version == consent.version }
        }

        if (hasOutdatedConsent) {
            throw RestApiException(
                exceptionCode = POLICY_VERSION_CHANGED,
                exceptionRecovery = ExceptionRecovery.refreshPolicies(
                    ConsentPolicy.entries.map { ConsentPolicyResponse.from(it) }
                )
            )
        }

        val hasMissingConsent = ConsentPolicy.entries
            .filter { it.required }
            .any { consentPolicy -> consents.none { it.policyId == consentPolicy.id } }

        if (hasMissingConsent) {
            throw RestApiException(CONSENT_REQUIRED)
        }
    }
}