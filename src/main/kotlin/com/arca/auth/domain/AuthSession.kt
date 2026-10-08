package com.arca.auth.domain

import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.PRE_PASSENGER
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "auth_sessions")
class AuthSession private constructor(
    tokenHash: String,
    sessionMode: SessionMode,
    passengerId: Long?,
    expiresAt: Instant,
    createdAt: Instant
) {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long = 0L
        protected set

    @Column(nullable = false, name = "token_hash")
    var tokenHash: String = tokenHash
        protected set

    @Enumerated(STRING)
    @Column(nullable = false, name = "session_mode")
    var sessionMode: SessionMode = sessionMode
        protected set

    @Column(name = "passenger_id")
    var passengerId: Long? = passengerId
        protected set

    @Column(nullable = false, name = "expires_at")
    var expiresAt: Instant = expiresAt
        protected set

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null
        protected set

    @Column(nullable = false, name = "created_at")
    var createdAt: Instant = createdAt
        protected set

    fun revoke(now: Instant) {
        if (revokedAt == null) {
            revokedAt = now
        }
    }

    fun isRevoked(): Boolean {
        return revokedAt != null
    }

    fun isExpired(now: Instant): Boolean {
        return !now.isBefore(expiresAt)
    }

    fun isAllowed(allowedSessionModes: Array<SessionMode>): Boolean {
        return sessionMode in allowedSessionModes
    }

    companion object {
        fun create(
            tokenHash: String,
            sessionMode: SessionMode,
            passengerId: Long?,
            expiresAt: Instant,
            createdAt: Instant
        ): AuthSession {
            validatePassengerId(
                sessionMode = sessionMode,
                passengerId = passengerId
            )

            return AuthSession(
                tokenHash = tokenHash,
                sessionMode = sessionMode,
                passengerId = passengerId,
                expiresAt = expiresAt,
                createdAt = createdAt
            )
        }

        private fun validatePassengerId(
            sessionMode: SessionMode,
            passengerId: Long?
        ) {
            val isValid = when (sessionMode) {
                PRE_PASSENGER -> passengerId == null
                ACTIVE -> passengerId != null
            }
            check(isValid) { "Passenger id does not match session mode. sessionMode = $sessionMode" }
        }
    }
}
