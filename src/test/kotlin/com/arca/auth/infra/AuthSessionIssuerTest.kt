package com.arca.auth.infra

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.GUEST
import com.arca.auth.repository.AuthSessionRepository
import com.arca.global.infra.IntegrationTest
import com.arca.global.property.AuthSessionProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@IntegrationTest
class AuthSessionIssuerTest(
    private val authSessionIssuer: AuthSessionIssuer,

    private val authSessionRepository: AuthSessionRepository,

    private val sha256Hasher: Sha256Hasher,
    private val authSessionProperties: AuthSessionProperties
) {

    @Nested
    inner class 토큰을_발급하면 {

        @Test
        fun 원문_대신_해시를_저장한다() {
            //when
            val issuedTokenDto = authSessionIssuer.issue(
                sessionMode = ACTIVE,
                memberId = MEMBER_ID,
                anonymousKeyHash = ANONYMOUS_KEY_HASH
            )

            //then
            val authSession = findByAccessToken(issuedTokenDto.accessToken)
            assertThat(authSession.tokenHash).isNotEqualTo(issuedTokenDto.accessToken)
            assertThat(authSession.sessionMode).isEqualTo(ACTIVE)
            assertThat(authSession.memberId).isEqualTo(MEMBER_ID)
        }

        @Test
        fun 세션을_익명_키_주체에_묶는다() {
            //when
            val issuedTokenDto = authSessionIssuer.issue(
                sessionMode = GUEST,
                memberId = null,
                anonymousKeyHash = ANONYMOUS_KEY_HASH
            )

            //then
            assertThat(findByAccessToken(issuedTokenDto.accessToken).anonymousKeyHash).isEqualTo(ANONYMOUS_KEY_HASH)
        }

        @Test
        fun 만료_시각은_발급_시각에서_TTL만큼_뒤다() {
            //when
            val issuedTokenDto = authSessionIssuer.issue(
                sessionMode = GUEST,
                memberId = null,
                anonymousKeyHash = ANONYMOUS_KEY_HASH
            )

            //then
            val authSession = findByAccessToken(issuedTokenDto.accessToken)
            assertThat(authSession.expiresAt).isEqualTo(authSession.createdAt.plus(authSessionProperties.ttl))
            assertThat(issuedTokenDto.expiresAt).isEqualTo(authSession.expiresAt)
        }

        @Test
        fun 발급할_때마다_다른_토큰을_만든다() {
            //when
            val firstIssuedTokenDto = authSessionIssuer.issue(
                sessionMode = GUEST,
                memberId = null,
                anonymousKeyHash = ANONYMOUS_KEY_HASH
            )
            val secondIssuedTokenDto = authSessionIssuer.issue(
                sessionMode = GUEST,
                memberId = null,
                anonymousKeyHash = ANONYMOUS_KEY_HASH
            )

            //then
            assertThat(secondIssuedTokenDto.accessToken).isNotEqualTo(firstIssuedTokenDto.accessToken)
        }
    }

    private fun findByAccessToken(accessToken: String): AuthSession {
        return checkNotNull(authSessionRepository.findByTokenHash(sha256Hasher.hash(accessToken)))
    }

    companion object {
        private const val MEMBER_ID = 1L
        private const val ANONYMOUS_KEY_HASH = "anonymous-key-hash"
    }
}
