package com.arca.auth.fixture

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.GUEST
import java.time.Instant

object AuthSessionFixture {
    private const val MEMBER_ID = 1L
    private const val ANONYMOUS_KEY_HASH = "anonymous-key-hash"
    private const val TOKEN_HASH = "token-hash"
    private val CREATED_AT = Instant.parse("2026-10-01T00:00:00Z")
    private val EXPIRES_AT = Instant.parse("2099-01-01T00:00:00Z")

    fun createActiveSession(
        tokenHash: String,
        expiresAt: Instant
    ): AuthSession {
        return AuthSession.create(MEMBER_ID, tokenHash, ANONYMOUS_KEY_HASH, ACTIVE, expiresAt, CREATED_AT)
    }

    fun createActiveSessionOf(memberId: Long): AuthSession {
        return AuthSession.create(memberId, TOKEN_HASH, ANONYMOUS_KEY_HASH, ACTIVE, EXPIRES_AT, CREATED_AT)
    }

    fun createGuestSession(): AuthSession {
        return AuthSession.create(null, TOKEN_HASH, ANONYMOUS_KEY_HASH, GUEST, EXPIRES_AT, CREATED_AT)
    }
}
