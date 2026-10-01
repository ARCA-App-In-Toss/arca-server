package com.arca.global.exception.dto.response

import com.arca.global.exception.domain.ExceptionCategory
import com.arca.global.exception.domain.ExceptionCode
import com.arca.global.exception.domain.ExceptionRecovery
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL

data class ErrorDetail(
    val code: ExceptionCode,

    val category: ExceptionCategory,

    val requestId: String,

    @field:JsonInclude(NON_NULL)
    val recovery: ExceptionRecovery?,

    @field:JsonInclude(NON_NULL)
    val retryAfterSeconds: Int?
)
