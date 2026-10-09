package com.arca.auth.service

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.fixture.AuthSessionFixture
import com.arca.auth.infra.AuthSessionIssuer
import com.arca.auth.infra.Sha256Hasher
import com.arca.auth.repository.AuthSessionRepository
import com.arca.global.exception.domain.ExceptionCode.SESSION_INVALID
import com.arca.global.exception.domain.ExceptionCode.SESSION_RECOVERY_REQUIRED
import com.arca.global.exception.domain.RestApiException
import com.arca.global.infra.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@IntegrationTest
class AuthSessionServiceTest(
    private val authSessionService: AuthSessionService,

    private val authSessionRepository: AuthSessionRepository,

    private val authSessionIssuer: AuthSessionIssuer,
    private val sha256Hasher: Sha256Hasher
) {

    @Nested
    inner class 토큰으로_인증할_때 {

        @Test
        fun 발급한_토큰이면_세션을_돌려준다() {
            //given
            val issuedTokenDto = authSessionIssuer.issue(
                sessionMode = ACTIVE,
                memberId = MEMBER_ID,
                anonymousKeyHash = ANONYMOUS_KEY_HASH
            )

            //when
            val authSession = authSessionService.authenticate(issuedTokenDto.accessToken)

            //then
            assertThat(authSession.sessionMode).isEqualTo(ACTIVE)
            assertThat(authSession.memberId).isEqualTo(MEMBER_ID)
        }

        @Test
        fun 모르는_토큰이면_SESSION_INVALID() {
            //when, then
            assertThatThrownBy { authSessionService.authenticate(UNKNOWN_TOKEN) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", SESSION_INVALID)
        }

        @Test
        fun 폐기된_토큰이면_SESSION_RECOVERY_REQUIRED() {
            //given
            val issuedTokenDto = authSessionIssuer.issue(
                sessionMode = ACTIVE,
                memberId = MEMBER_ID,
                anonymousKeyHash = ANONYMOUS_KEY_HASH
            )
            authSessionService.revoke(findByAccessToken(issuedTokenDto.accessToken).id)

            //when, then
            assertThatThrownBy { authSessionService.authenticate(issuedTokenDto.accessToken) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", SESSION_RECOVERY_REQUIRED)
        }

        @Test
        fun 만료된_토큰이면_SESSION_RECOVERY_REQUIRED() {
            //given
            val authSession = AuthSessionFixture.createActiveSession(
                sha256Hasher.hash(EXPIRED_TOKEN),
                Instant.parse(EXPIRED_AT)
            )
            authSessionRepository.save(authSession)

            //when, then
            assertThatThrownBy { authSessionService.authenticate(EXPIRED_TOKEN) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", SESSION_RECOVERY_REQUIRED)
        }
    }

    @Nested
    inner class 세션을_폐기할_때 {

        @Test
        fun 폐기_시각을_기록한다() {
            //given
            val issuedTokenDto = authSessionIssuer.issue(
                sessionMode = ACTIVE,
                memberId = MEMBER_ID,
                anonymousKeyHash = ANONYMOUS_KEY_HASH
            )
            val authSession = findByAccessToken(issuedTokenDto.accessToken)

            //when
            authSessionService.revoke(authSession.id)

            //then
            assertThat(findByAccessToken(issuedTokenDto.accessToken).revokedAt).isNotNull()
        }

        @Test
        fun 없는_세션이면_SESSION_INVALID() {
            //when, then
            assertThatThrownBy { authSessionService.revoke(UNKNOWN_SESSION_ID) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", SESSION_INVALID)
        }
    }

    private fun findByAccessToken(accessToken: String): AuthSession {
        return checkNotNull(authSessionRepository.findByTokenHash(sha256Hasher.hash(accessToken)))
    }

    companion object {
        private const val MEMBER_ID = 1L
        private const val ANONYMOUS_KEY_HASH = "anonymous-key-hash"
        private const val UNKNOWN_SESSION_ID = -1L
        private const val UNKNOWN_TOKEN = "unknown-token"
        private const val EXPIRED_TOKEN = "expired-token"
        private const val EXPIRED_AT = "2026-01-01T00:00:00Z"
    }
}
