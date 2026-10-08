package com.arca.auth.fixture

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import java.time.Instant

object AuthSessionFixture {
    private const val MEMBER_ID = 1L
    private val CREATED_AT = Instant.parse("2026-10-01T00:00:00Z")

    fun createActiveSession(
        tokenHash: String,
        expiresAt: Instant
    ): AuthSession {
        return AuthSession.create(tokenHash, ACTIVE, MEMBER_ID, expiresAt, CREATED_AT)
    }
}
