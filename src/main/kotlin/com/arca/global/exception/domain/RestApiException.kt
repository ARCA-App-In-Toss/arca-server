package com.arca.global.exception.domain

import com.arca.global.exception.domain.ExceptionCode.MAINTENANCE
import com.arca.global.exception.domain.ExceptionCode.RATE_LIMITED

class RestApiException(
    val exceptionCode: ExceptionCode,
    val exceptionRecovery: ExceptionRecovery? = null,
    val retryAfterSeconds: Int? = null
) : RuntimeException(exceptionCode.name) {

    init {
        check(exceptionRecovery?.kind == exceptionCode.exceptionRecoveryKind) {
            "${exceptionCode}의 recovery는 ${exceptionCode.exceptionRecoveryKind}여야 한다. 실제: ${exceptionRecovery?.kind}"
        }
        check(retryAfterSeconds == null || exceptionCode in RETRY_AFTER_CODES) {
            "${exceptionCode}에는 retryAfterSeconds를 실을 수 없다"
        }
    }

    companion object {
        private val RETRY_AFTER_CODES = setOf(RATE_LIMITED, MAINTENANCE)
    }
}
