package com.arca.auth.service

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode
import com.arca.auth.domain.SessionMode.GUEST
import com.arca.auth.dto.internal.IssuedTokenDto
import com.arca.auth.dto.request.CreateSessionRequest
import com.arca.auth.dto.response.CreateSessionResponse
import com.arca.auth.dto.response.SessionContextResponse
import com.arca.auth.infra.AccessTokenGenerator
import com.arca.auth.infra.AccessTokenHasher
import com.arca.auth.infra.AnonymousKeyVerifier
import com.arca.auth.repository.AuthSessionRepository
import com.arca.consent.domain.ConsentPolicy
import com.arca.consent.dto.response.ConsentPolicyResponse
import com.arca.global.exception.domain.ExceptionCode.ANONYMOUS_KEY_INVALID
import com.arca.global.exception.domain.ExceptionCode.SESSION_INVALID
import com.arca.global.exception.domain.ExceptionCode.SESSION_RECOVERY_REQUIRED
import com.arca.global.exception.domain.ExceptionRecovery
import com.arca.global.exception.domain.RestApiException
import com.arca.global.property.AuthSessionProperties
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class AuthSessionService (
    private val authSessionRepository: AuthSessionRepository,

    private val accessTokenGenerator: AccessTokenGenerator,
    private val accessTokenHasher: AccessTokenHasher,
    private val anonymousKeyVerifier: AnonymousKeyVerifier,
    private val authSessionProperties: AuthSessionProperties,

    private val clock: Clock
){
    @Transactional
    fun createSession(request: CreateSessionRequest): CreateSessionResponse {
        if(!anonymousKeyVerifier.verify(request.anonymousKey)){
            throw RestApiException(ANONYMOUS_KEY_INVALID)
        }

        val issuedTokenDto = issue(
            sessionMode = GUEST,
            memberId = null
        )

        return CreateSessionResponse.of(
            issuedTokenDto = issuedTokenDto,
            context = SessionContextResponse.of(
                sessionMode = GUEST,
                member = null
            ),
            consentPolicies = ConsentPolicy.entries.map { ConsentPolicyResponse.from(it) }
        )
    }

    @Transactional
    fun issue(
        sessionMode: SessionMode,
        memberId: Long?
    ): IssuedTokenDto {
        val now = clock.instant()
        val accessToken = accessTokenGenerator.generate()
        val expiresAt = now.plus(authSessionProperties.ttl)

        val authSession = AuthSession.create(
            tokenHash = accessTokenHasher.hash(accessToken),
            sessionMode = sessionMode,
            memberId = memberId,
            expiresAt = expiresAt,
            createdAt = now
        )

        authSessionRepository.save(authSession)

        return IssuedTokenDto(
            accessToken = accessToken,
            expiresAt = expiresAt
        )
    }

    @Transactional
    fun revoke(authSessionId: Long){
        val authSession = authSessionRepository.findByIdOrNull(authSessionId)
            ?: throw RestApiException(SESSION_INVALID)

        authSession.revoke(clock.instant())
    }

    @Transactional(readOnly = true)
    fun authenticate(accessToken: String): AuthSession {
        val authSession = authSessionRepository.findByTokenHash(accessTokenHasher.hash(accessToken))
            ?: throw RestApiException(SESSION_INVALID)

        if(authSession.isRevoked() || authSession.isExpired(clock.instant())){
            throw RestApiException(
                exceptionCode = SESSION_RECOVERY_REQUIRED,
                exceptionRecovery = ExceptionRecovery.reestablishSession()
            )
        }

        return authSession
    }
}