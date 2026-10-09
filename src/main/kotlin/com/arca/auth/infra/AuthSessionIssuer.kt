package com.arca.auth.infra

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode
import com.arca.auth.dto.internal.IssuedTokenDto
import com.arca.auth.repository.AuthSessionRepository
import com.arca.global.property.AuthSessionProperties
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64

@Component
class AuthSessionIssuer(
    private val authSessionRepository: AuthSessionRepository,

    private val sha256Hasher: Sha256Hasher,
    private val authSessionProperties: AuthSessionProperties,

    private val clock: Clock
) {
    fun issue(
        sessionMode: SessionMode,
        memberId: Long?,
        anonymousKeyHash: String
    ): IssuedTokenDto {
        val now = clock.instant()
        val accessToken = generateAccessToken()
        val expiresAt = now.plus(authSessionProperties.ttl)

        val authSession = AuthSession.create(
            memberId = memberId,
            tokenHash = sha256Hasher.hash(accessToken),
            anonymousKeyHash = anonymousKeyHash,
            sessionMode = sessionMode,
            expiresAt = expiresAt,
            createdAt = now
        )

        authSessionRepository.save(authSession)

        return IssuedTokenDto(
            accessToken = accessToken,
            expiresAt = expiresAt
        )
    }

    private fun generateAccessToken(): String {
        val tokenBytes = ByteArray(TOKEN_BYTE_LENGTH)
        SECURE_RANDOM.nextBytes(tokenBytes)

        return BASE64_URL_ENCODER.encodeToString(tokenBytes)
    }

    companion object {
        private const val TOKEN_BYTE_LENGTH = 32
        private val SECURE_RANDOM = SecureRandom()
        private val BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding()
    }
}