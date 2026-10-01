package com.arca.global.infra

import com.arca.global.exception.domain.ExceptionCode.ANSWER_NOT_FOUND
import com.arca.global.exception.domain.ExceptionCode.COMMAND_ALREADY_PENDING
import com.arca.global.exception.domain.ExceptionCode.RATE_LIMITED
import com.arca.global.exception.domain.ExceptionCode.SESSION_RECOVERY_REQUIRED
import com.arca.global.exception.domain.ExceptionRecovery
import com.arca.global.exception.domain.RestApiException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

/**
 * 전역 설정(Jackson, CORS, Cache-Control)과 공통 예외 처리를 HTTP 경로로 검증하기 위한 테스트 전용 컨트롤러.
 */
@RestController
@RequestMapping("/v1/test")
class TestController {

    @GetMapping
    fun get(): TestResponse {
        return TestResponse(
            name = null,
            createdAt = Instant.parse(CREATED_AT)
        )
    }

    @PostMapping
    fun post(@Valid @RequestBody request: TestRequest): TestResponse {
        return TestResponse(
            name = request.name,
            createdAt = Instant.parse(CREATED_AT)
        )
    }

    @GetMapping("/items/{itemId}")
    fun getItem(
        @PathVariable itemId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: UUID
    ): TestResponse {
        return TestResponse(
            name = "$itemId $idempotencyKey",
            createdAt = Instant.parse(CREATED_AT)
        )
    }

    @GetMapping("/exceptions/plain")
    fun throwPlain(): TestResponse {
        throw RestApiException(ANSWER_NOT_FOUND)
    }

    @GetMapping("/exceptions/session-recovery")
    fun throwSessionRecovery(): TestResponse {
        throw RestApiException(
            exceptionCode = SESSION_RECOVERY_REQUIRED,
            exceptionRecovery = ExceptionRecovery.reestablishSession()
        )
    }

    @GetMapping("/exceptions/command-pending")
    fun throwCommandPending(): TestResponse {
        throw RestApiException(
            exceptionCode = COMMAND_ALREADY_PENDING,
            exceptionRecovery = ExceptionRecovery.queryCommand(TICKET_ID)
        )
    }

    @GetMapping("/exceptions/rate-limited")
    fun throwRateLimited(): TestResponse {
        throw RestApiException(
            exceptionCode = RATE_LIMITED,
            retryAfterSeconds = RETRY_AFTER_SECONDS
        )
    }

    @GetMapping("/exceptions/unexpected")
    fun throwUnexpected(): TestResponse {
        throw IllegalStateException(UNEXPECTED_MESSAGE)
    }

    data class TestRequest(
        @field:NotBlank
        val name: String
    )

    data class TestResponse(
        val name: String?,
        val createdAt: Instant
    )

    companion object {
        const val CREATED_AT = "2026-09-13T14:59:00Z"
        const val TICKET_ID = "ticket-1"
        const val RETRY_AFTER_SECONDS = 30
        const val UNEXPECTED_MESSAGE = "노출되면 안 되는 내부 원인"
    }
}
