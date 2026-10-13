package com.arca.auth.repository

import com.arca.auth.domain.AuthSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface AuthSessionRepository : JpaRepository<AuthSession, Long> {
    @Query("""
        SELECT s
        FROM AuthSession s
        WHERE s.tokenHash = :tokenHash
    """)
    fun findByTokenHash(
        @Param("tokenHash") tokenHash: String
    ): AuthSession?

    @Query("""
        SELECT s
        FROM AuthSession s
        WHERE s.memberId = :memberId
          AND s.revokedAt IS NULL
          AND s.expiresAt > :now
    """)
    fun findAllValidByMemberId(
        @Param("memberId") memberId: Long,
        @Param("now") now: Instant
    ): List<AuthSession>
}