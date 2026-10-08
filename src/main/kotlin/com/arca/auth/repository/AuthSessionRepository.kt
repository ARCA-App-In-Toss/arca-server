package com.arca.auth.repository

import com.arca.auth.domain.AuthSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AuthSessionRepository : JpaRepository<AuthSession, Long> {
    @Query("""
        SELECT s
        FROM AuthSession s
        WHERE s.tokenHash = :tokenHash
    """)
    fun findByTokenHash(
        @Param("tokenHash") tokenHash: String
    ): AuthSession?
}