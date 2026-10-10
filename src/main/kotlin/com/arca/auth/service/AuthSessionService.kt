package com.arca.auth.service

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.GUEST
import com.arca.auth.dto.request.CreateSessionRequest
import com.arca.auth.dto.response.SessionContextResponse
import com.arca.auth.dto.response.SessionResponse
import com.arca.auth.infra.AnonymousKeyVerifier
import com.arca.auth.infra.AuthSessionIssuer
import com.arca.auth.infra.Sha256Hasher
import com.arca.auth.repository.AuthSessionRepository
import com.arca.global.exception.domain.ExceptionCode.ANONYMOUS_KEY_INVALID
import com.arca.global.exception.domain.ExceptionCode.SESSION_INVALID
import com.arca.global.exception.domain.ExceptionCode.SESSION_RECOVERY_REQUIRED
import com.arca.global.exception.domain.ExceptionRecovery
import com.arca.global.exception.domain.RestApiException
import com.arca.member.dto.response.MemberProfileResponse
import com.arca.member.repository.MemberRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class AuthSessionService(
    private val authSessionRepository: AuthSessionRepository,
    private val memberRepository: MemberRepository,

    private val authSessionIssuer: AuthSessionIssuer,
    private val sha256Hasher: Sha256Hasher,
    private val anonymousKeyVerifier: AnonymousKeyVerifier,

    private val clock: Clock
) {
    @Transactional
    fun createSession(request: CreateSessionRequest): SessionResponse {
        if (!anonymousKeyVerifier.verify(request.anonymousKey)) {
            throw RestApiException(ANONYMOUS_KEY_INVALID)
        }

        val anonymousKeyHash = sha256Hasher.hash(request.anonymousKey)
        val member = memberRepository.findByAnonymousKeyHash(anonymousKeyHash)
        val sessionMode = if (member == null) GUEST else ACTIVE

        val issuedTokenDto = authSessionIssuer.issue(
            sessionMode = sessionMode,
            memberId = member?.id,
            anonymousKeyHash = anonymousKeyHash
        )

        return SessionResponse.of(
            issuedTokenDto = issuedTokenDto,
            context = SessionContextResponse.of(
                sessionMode = sessionMode,
                member = member?.let { MemberProfileResponse.from(it) }
            )
        )
    }

    @Transactional
    fun revoke(authSessionId: Long) {
        val authSession = authSessionRepository.findByIdOrNull(authSessionId)
            ?: throw RestApiException(SESSION_INVALID)

        authSession.revoke(clock.instant())
    }

    @Transactional(readOnly = true)
    fun authenticate(accessToken: String): AuthSession {
        val tokenHash = sha256Hasher.hash(accessToken)
        val authSession = authSessionRepository.findByTokenHash(tokenHash)
            ?: throw RestApiException(SESSION_INVALID)

        if (authSession.isRevoked() || authSession.isExpired(clock.instant())) {
            throw RestApiException(
                exceptionCode = SESSION_RECOVERY_REQUIRED,
                exceptionRecovery = ExceptionRecovery.reestablishSession()
            )
        }

        return authSession
    }
}