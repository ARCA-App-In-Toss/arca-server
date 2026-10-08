package com.arca.auth.infra

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!prod")
class StubAnonymousKeyVerifier: AnonymousKeyVerifier {
    override fun verify(anonymousKey: String): Boolean {
        return true
    }
}