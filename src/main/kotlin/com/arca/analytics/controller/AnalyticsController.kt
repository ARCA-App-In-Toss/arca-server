package com.arca.analytics.controller

import com.arca.analytics.dto.request.CreateEventBatchRequest
import com.arca.analytics.service.AnalyticsService
import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.GUEST
import com.arca.global.annotation.AllowedSessionModes
import com.arca.global.annotation.Auth
import jakarta.validation.Valid
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/analytics")
class AnalyticsController(
    private val analyticsService: AnalyticsService
) : AnalyticsControllerDocs {

    @AllowedSessionModes([GUEST, ACTIVE])
    @PostMapping("/event-batches")
    override fun createEventBatch(
        @Auth authSession: AuthSession,
        @Valid @RequestBody request: CreateEventBatchRequest
    ): ResponseEntity<Void> {
        analyticsService.createEventBatch(authSession, request)
        return ResponseEntity.status(ACCEPTED).build()
    }
}
