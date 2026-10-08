package com.arca.global.infra

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.global.annotation.AllowedSessionModes
import com.arca.global.annotation.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/test-auth")
class AuthTestController {

    @AllowedSessionModes([ACTIVE])
    @GetMapping("/active")
    fun getActive(
        @Auth authSession: AuthSession
    ): String {
        return authSession.sessionMode.name
    }

    @GetMapping("/public")
    fun getPublic(): String {
        return PUBLIC_BODY
    }

    companion object {
        const val PUBLIC_BODY = "public"
    }
}
