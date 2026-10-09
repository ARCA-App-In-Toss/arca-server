package com.arca.auth.domain

import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.GUEST
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
@Table(name = "auth_session")
class AuthSession private constructor(
    memberId: Long?,
    tokenHash: String,
    anonymousKeyHash: String,
    sessionMode: SessionMode,
    expiresAt: Instant,
    createdAt: Instant
) {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long = 0L
        protected set

    @Column(name = "member_id")
    var memberId: Long? = memberId
        protected set

    @Column(nullable = false, name = "token_hash")
    var tokenHash: String = tokenHash
        protected set

    @Column(nullable = false, name = "anonymous_key_hash")
    var anonymousKeyHash: String = anonymousKeyHash
        protected set

    @Enumerated(STRING)
    @Column(nullable = false, name = "session_mode")
    var sessionMode: SessionMode = sessionMode
        protected set

    @Column(nullable = false, name = "created_at")
    var createdAt: Instant = createdAt
        protected set

    @Column(nullable = false, name = "expires_at")
    var expiresAt: Instant = expiresAt
        protected set

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null
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
            memberId: Long?,
            tokenHash: String,
            anonymousKeyHash: String,
            sessionMode: SessionMode,
            expiresAt: Instant,
            createdAt: Instant
        ): AuthSession {
            validateMemberId(
                sessionMode = sessionMode,
                memberId = memberId
            )

            return AuthSession(
                memberId = memberId,
                tokenHash = tokenHash,
                anonymousKeyHash = anonymousKeyHash,
                sessionMode = sessionMode,
                expiresAt = expiresAt,
                createdAt = createdAt
            )
        }

        private fun validateMemberId(
            sessionMode: SessionMode,
            memberId: Long?
        ) {
            val isValid = when (sessionMode) {
                GUEST -> memberId == null
                ACTIVE -> memberId != null
            }
            check(isValid) { "Member id does not match session mode. sessionMode = $sessionMode" }
        }
    }
}
