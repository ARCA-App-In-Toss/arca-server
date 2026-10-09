package com.arca.auth.controller

import com.arca.auth.dto.request.CreateSessionRequest
import com.arca.auth.dto.response.SessionResponse
import com.arca.auth.service.AuthSessionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/sessions")
class AuthSessionController (
    private val authSessionService: AuthSessionService
): AuthSessionControllerDocs {

    @PostMapping
    override fun createSession(
        @Valid @RequestBody request: CreateSessionRequest
    ): ResponseEntity<SessionResponse>{
        val response = authSessionService.createSession(request)
        return ResponseEntity.status(CREATED).body(response)
    }
}