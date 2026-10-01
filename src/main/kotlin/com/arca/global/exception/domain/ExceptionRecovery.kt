package com.arca.global.exception.domain

import com.arca.global.exception.domain.ExceptionRecoveryKind.QUERY_COMMAND
import com.arca.global.exception.domain.ExceptionRecoveryKind.REESTABLISH_SESSION
import com.arca.global.exception.domain.ExceptionRecoveryKind.REFRESH_TODAY
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL

class ExceptionRecovery private constructor(
    val kind: ExceptionRecoveryKind,

    @field:JsonInclude(NON_NULL)
    val recoveryAllowed: Boolean?,

    @field:JsonInclude(NON_NULL)
    val ticketId: String?
) {
    companion object {
        fun reestablishSession(): ExceptionRecovery {
            return ExceptionRecovery(
                kind = REESTABLISH_SESSION,
                recoveryAllowed = true,
                ticketId = null
            )
        }

        fun refreshToday(): ExceptionRecovery {
            return ExceptionRecovery(
                kind = REFRESH_TODAY,
                recoveryAllowed = null,
                ticketId = null
            )
        }

        fun queryCommand(ticketId: String): ExceptionRecovery {
            return ExceptionRecovery(
                kind = QUERY_COMMAND,
                recoveryAllowed = null,
                ticketId = ticketId
            )
        }
    }
}
