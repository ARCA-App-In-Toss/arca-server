package com.arca.auth.infra

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class FakeAnonymousKeyVerifier : AnonymousKeyVerifier {

    override fun verify(anonymousKey: String): Boolean {
        return anonymousKey != INVALID_ANONYMOUS_KEY
    }

    companion object {
        const val INVALID_ANONYMOUS_KEY = "invalid-anonymous-key"
    }
}
