package com.arca.global.infra

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 전역 설정(Jackson, CORS, Cache-Control)을 HTTP 경로로 검증하기 위한 테스트 전용 컨트롤러.
 */
@RestController
@RequestMapping("/v1/test")
class TestController {

    @GetMapping
    fun get(): TestResponse {
        return TestResponse(
            name = null,
            createdAt = Instant.parse(CREATED_AT),
        )
    }

    @PostMapping
    fun post(@RequestBody request: TestRequest): TestResponse {
        return TestResponse(
            name = request.name,
            createdAt = Instant.parse(CREATED_AT),
        )
    }

    data class TestRequest(
        val name: String,
    )

    data class TestResponse(
        val name: String?,
        val createdAt: Instant,
    )

    companion object {
        const val CREATED_AT = "2026-09-13T14:59:00Z"
    }
}
