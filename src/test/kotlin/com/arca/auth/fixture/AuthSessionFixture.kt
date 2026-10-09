package com.arca.auth.fixture

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import java.time.Instant

object AuthSessionFixture {
    private const val MEMBER_ID = 1L
    private const val ANONYMOUS_KEY_HASH = "anonymous-key-hash"
    private val CREATED_AT = Instant.parse("2026-10-01T00:00:00Z")

    fun createActiveSession(
        tokenHash: String,
        expiresAt: Instant
    ): AuthSession {
        return AuthSession.create(MEMBER_ID, tokenHash, ANONYMOUS_KEY_HASH, ACTIVE, expiresAt, CREATED_AT)
    }
}
