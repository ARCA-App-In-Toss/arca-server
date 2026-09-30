package com.arca.global.exception.domain

import com.arca.global.exception.domain.ExceptionRecoveryKind.QUERY_COMMAND
import com.arca.global.exception.domain.ExceptionRecoveryKind.REESTABLISH_SESSION
import com.arca.global.exception.domain.ExceptionRecoveryKind.REFRESH_POLICIES
import com.arca.global.exception.domain.ExceptionRecoveryKind.REFRESH_TODAY
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

enum class ExceptionCode(
    val httpStatus: HttpStatus,
    val exceptionCategory: ExceptionCategory,
    val exceptionRecoveryKind: ExceptionRecoveryKind? = null,
) {
    // 요청 형식, 값 검증 (VALIDATION)
    INVALID_REQUEST(BAD_REQUEST, ExceptionCategory.VALIDATION),
    CURSOR_INVALID(BAD_REQUEST, ExceptionCategory.VALIDATION),
    PASSENGER_NOT_FOUND(NOT_FOUND, ExceptionCategory.VALIDATION),
    ANSWER_NOT_FOUND(NOT_FOUND, ExceptionCategory.VALIDATION),
    COMMAND_NOT_FOUND(NOT_FOUND, ExceptionCategory.VALIDATION),
    CONSENT_REQUIRED(UNPROCESSABLE_ENTITY, ExceptionCategory.VALIDATION),
    POLICY_VERSION_CHANGED(UNPROCESSABLE_ENTITY, ExceptionCategory.VALIDATION, REFRESH_POLICIES),
    NICKNAME_INVALID(UNPROCESSABLE_ENTITY, ExceptionCategory.VALIDATION),
    DATE_CHANGED(UNPROCESSABLE_ENTITY, ExceptionCategory.VALIDATION, REFRESH_TODAY),
    SEMA_REPLACED(UNPROCESSABLE_ENTITY, ExceptionCategory.VALIDATION, REFRESH_TODAY),
    COMMAND_PAYLOAD_MISMATCH(UNPROCESSABLE_ENTITY, ExceptionCategory.VALIDATION),

    // 인증 (AUTH)
    ANONYMOUS_KEY_INVALID(UNAUTHORIZED, ExceptionCategory.AUTH),
    SESSION_RECOVERY_REQUIRED(UNAUTHORIZED, ExceptionCategory.AUTH, REESTABLISH_SESSION),
    SESSION_INVALID(UNAUTHORIZED, ExceptionCategory.AUTH),
    SESSION_SCOPE_INSUFFICIENT(FORBIDDEN, ExceptionCategory.AUTH),

    // 충돌 (CONFLICT)
    PASSENGER_ALREADY_EXISTS(HttpStatus.CONFLICT, ExceptionCategory.CONFLICT),
    ANSWER_ALREADY_EXISTS(HttpStatus.CONFLICT, ExceptionCategory.CONFLICT),
    REVISION_CONFLICT(HttpStatus.CONFLICT, ExceptionCategory.CONFLICT),
    COMMAND_ALREADY_PENDING(HttpStatus.CONFLICT, ExceptionCategory.CONFLICT, QUERY_COMMAND),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT, ExceptionCategory.CONFLICT),
    COMMAND_NOT_TERMINAL(HttpStatus.CONFLICT, ExceptionCategory.CONFLICT),
    OPERATION_RESULT_EXPIRED(HttpStatus.CONFLICT, ExceptionCategory.CONFLICT),

    // 요청 제한 (RATE_LIMIT)
    RATE_LIMITED(TOO_MANY_REQUESTS, ExceptionCategory.RATE_LIMIT),

    // 서버 오류, 점검 (MAINTENANCE)
    INTERNAL_ERROR(INTERNAL_SERVER_ERROR, ExceptionCategory.MAINTENANCE),
    MAINTENANCE(SERVICE_UNAVAILABLE, ExceptionCategory.MAINTENANCE),
}
