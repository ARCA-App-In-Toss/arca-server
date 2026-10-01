package com.arca.global.exception.dto.response

import com.arca.global.exception.domain.ExceptionCode
import com.arca.global.exception.domain.ExceptionRecovery

data class ErrorResponse(
    val error: ErrorDetail
) {
    companion object {
        fun of(
            exceptionCode: ExceptionCode,
            requestId: String,
            exceptionRecovery: ExceptionRecovery?,
            retryAfterSeconds: Int?
        ): ErrorResponse {
            return ErrorResponse(
                error = ErrorDetail(
                    code = exceptionCode,
                    category = exceptionCode.exceptionCategory,
                    requestId = requestId,
                    recovery = exceptionRecovery,
                    retryAfterSeconds = retryAfterSeconds
                )
            )
        }
    }
}
