package com.arca.semaphore.controller

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.global.annotation.AllowedSessionModes
import com.arca.global.annotation.Auth
import com.arca.semaphore.dto.response.TodaySemaphoreResponse
import com.arca.semaphore.service.SemaphoreService
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/semas")
class SemaphoreController(
    private val semaphoreService: SemaphoreService
) : SemaphoreControllerDocs {

    @AllowedSessionModes([ACTIVE])
    @GetMapping("/today")
    override fun getTodaySemaphore(
        @Auth authSession: AuthSession
    ): ResponseEntity<TodaySemaphoreResponse> {
        val response = semaphoreService.getTodaySemaphore(authSession)
        return ResponseEntity.status(OK).body(response)
    }
}