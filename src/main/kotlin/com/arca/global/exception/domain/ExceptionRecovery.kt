package com.arca.global.exception.domain

import com.arca.consent.dto.response.ConsentPolicyResponse
import com.arca.global.exception.domain.ExceptionRecoveryKind.REESTABLISH_SESSION
import com.arca.global.exception.domain.ExceptionRecoveryKind.REFRESH_POLICIES
import com.arca.global.exception.domain.ExceptionRecoveryKind.REFRESH_TODAY
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL

class ExceptionRecovery private constructor(
    val kind: ExceptionRecoveryKind,

    @field:JsonInclude(NON_NULL)
    val recoveryAllowed: Boolean?,

    @field:JsonInclude(NON_NULL)
    val policies: List<ConsentPolicyResponse>?
) {
    companion object {
        fun reestablishSession(): ExceptionRecovery {
            return ExceptionRecovery(
                kind = REESTABLISH_SESSION,
                recoveryAllowed = true,
                policies = null
            )
        }

        fun refreshPolicies(policies: List<ConsentPolicyResponse>): ExceptionRecovery {
            return ExceptionRecovery(
                kind = REFRESH_POLICIES,
                recoveryAllowed = null,
                policies = policies
            )
        }

        fun refreshToday(): ExceptionRecovery {
            return ExceptionRecovery(
                kind = REFRESH_TODAY,
                recoveryAllowed = null,
                policies = null
            )
        }
    }
}
