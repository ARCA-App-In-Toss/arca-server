package com.arca.auth.service

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.PRE_PASSENGER
import com.arca.auth.fixture.AuthSessionFixture
import com.arca.auth.infra.AccessTokenHasher
import com.arca.auth.repository.AuthSessionRepository
import com.arca.global.exception.domain.ExceptionCode.SESSION_INVALID
import com.arca.global.exception.domain.ExceptionCode.SESSION_RECOVERY_REQUIRED
import com.arca.global.exception.domain.RestApiException
import com.arca.global.infra.IntegrationTest
import com.arca.global.property.AuthSessionProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@IntegrationTest
class AuthSessionServiceTest(
    private val authSessionService: AuthSessionService,

    private val authSessionRepository: AuthSessionRepository,

    private val accessTokenHasher: AccessTokenHasher,
    private val authSessionProperties: AuthSessionProperties
) {

    @Nested
    inner class 토큰을_발급하면 {

        @Test
        fun 원문_대신_해시를_저장한다() {
            //when
            val issuedTokenDto = authSessionService.issue(
                sessionMode = ACTIVE,
                passengerId = PASSENGER_ID
            )

            //then
            val authSession = findByAccessToken(issuedTokenDto.accessToken)
            assertThat(authSession.tokenHash).isNotEqualTo(issuedTokenDto.accessToken)
            assertThat(authSession.sessionMode).isEqualTo(ACTIVE)
            assertThat(authSession.passengerId).isEqualTo(PASSENGER_ID)
        }

        @Test
        fun 만료_시각은_발급_시각에서_TTL만큼_뒤다() {
            //when
            val issuedTokenDto = authSessionService.issue(
                sessionMode = PRE_PASSENGER,
                passengerId = null
            )

            //then
            val authSession = findByAccessToken(issuedTokenDto.accessToken)
            assertThat(authSession.expiresAt).isEqualTo(authSession.createdAt.plus(authSessionProperties.ttl))
            assertThat(issuedTokenDto.expiresAt).isEqualTo(authSession.expiresAt)
        }
    }

    @Nested
    inner class 토큰으로_인증할_때 {

        @Test
        fun 발급한_토큰이면_세션을_돌려준다() {
            //given
            val issuedTokenDto = authSessionService.issue(
                sessionMode = ACTIVE,
                passengerId = PASSENGER_ID
            )

            //when
            val authSession = authSessionService.authenticate(issuedTokenDto.accessToken)

            //then
            assertThat(authSession.sessionMode).isEqualTo(ACTIVE)
            assertThat(authSession.passengerId).isEqualTo(PASSENGER_ID)
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
            val issuedTokenDto = authSessionService.issue(
                sessionMode = ACTIVE,
                passengerId = PASSENGER_ID
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
                accessTokenHasher.hash(EXPIRED_TOKEN),
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
            val issuedTokenDto = authSessionService.issue(
                sessionMode = ACTIVE,
                passengerId = PASSENGER_ID
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
        return checkNotNull(authSessionRepository.findByTokenHash(accessTokenHasher.hash(accessToken)))
    }

    companion object {
        private const val PASSENGER_ID = 1L
        private const val UNKNOWN_SESSION_ID = -1L
        private const val UNKNOWN_TOKEN = "unknown-token"
        private const val EXPIRED_TOKEN = "expired-token"
        private const val EXPIRED_AT = "2026-01-01T00:00:00Z"
    }
}
