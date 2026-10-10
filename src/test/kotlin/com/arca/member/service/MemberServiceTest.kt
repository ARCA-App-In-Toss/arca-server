package com.arca.member.service

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.GUEST
import com.arca.auth.infra.AuthSessionIssuer
import com.arca.auth.service.AuthSessionService
import com.arca.consent.dto.request.ConsentRequest
import com.arca.global.exception.domain.ExceptionCode.MEMBER_ALREADY_EXISTS
import com.arca.global.exception.domain.ExceptionCode.POLICY_VERSION_CHANGED
import com.arca.global.exception.domain.ExceptionRecoveryKind.REFRESH_POLICIES
import com.arca.global.exception.domain.RestApiException
import com.arca.global.infra.IntegrationTest
import com.arca.member.dto.request.CreateMemberRequest
import com.arca.member.fixture.MemberFixture
import com.arca.member.infra.FakeMemberCodeGenerator
import com.arca.member.repository.MemberRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@IntegrationTest
class MemberServiceTest(
    private val memberService: MemberService,

    private val memberRepository: MemberRepository,

    private val authSessionIssuer: AuthSessionIssuer,
    private val authSessionService: AuthSessionService,
    private val fakeMemberCodeGenerator: FakeMemberCodeGenerator
) {

    @AfterEach
    fun tearDown() {
        fakeMemberCodeGenerator.clear()
    }

    @Nested
    inner class 회원_코드를_배정할_때 {

        @Test
        fun ARCA_뒤에_대문자_영어와_숫자_5자를_붙인다() {
            //when
            val sessionResponse = memberService.createMember(guestSessionOf(ANONYMOUS_KEY_HASH), validRequest())

            //then
            assertThat(checkNotNull(sessionResponse.context.member).memberCode).matches(MEMBER_CODE_PATTERN)
        }

        @Test
        fun 회원마다_다른_코드를_배정한다() {
            //when
            val firstSessionResponse = memberService.createMember(guestSessionOf(ANONYMOUS_KEY_HASH), validRequest())
            val secondSessionResponse = memberService.createMember(guestSessionOf(OTHER_ANONYMOUS_KEY_HASH), validRequest())

            //then
            assertThat(checkNotNull(secondSessionResponse.context.member).memberCode)
                .isNotEqualTo(checkNotNull(firstSessionResponse.context.member).memberCode)
        }

        @Test
        fun 이미_있는_코드가_나오면_다시_생성한다() {
            //given
            memberRepository.save(MemberFixture.createMemberWithDetails(TAKEN_MEMBER_CODE, OTHER_ANONYMOUS_KEY_HASH))
            fakeMemberCodeGenerator.reserve(TAKEN_MEMBER_CODE, FREE_MEMBER_CODE)

            //when
            val sessionResponse = memberService.createMember(guestSessionOf(ANONYMOUS_KEY_HASH), validRequest())

            //then
            assertThat(checkNotNull(sessionResponse.context.member).memberCode).isEqualTo(FREE_MEMBER_CODE)
        }
    }

    @Nested
    inner class 가입을_판정할_때 {

        @Test
        fun ACTIVE_세션이면_정책_검증보다_MEMBER_ALREADY_EXISTS가_먼저다() {
            //given
            val member = memberRepository.save(MemberFixture.createMember(ANONYMOUS_KEY_HASH))
            val activeSession = activeSessionOf(member.id, ANONYMOUS_KEY_HASH)

            //when, then
            assertThatThrownBy { memberService.createMember(activeSession, outdatedRequest()) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_ALREADY_EXISTS)
        }

        @Test
        fun 같은_키의_회원이_있으면_정책_검증보다_MEMBER_ALREADY_EXISTS가_먼저다() {
            //given
            memberRepository.save(MemberFixture.createMember(ANONYMOUS_KEY_HASH))

            //when, then
            assertThatThrownBy { memberService.createMember(guestSessionOf(ANONYMOUS_KEY_HASH), outdatedRequest()) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_ALREADY_EXISTS)
        }

        @Test
        fun 버전_불일치와_필수_정책_누락이_같이_있으면_POLICY_VERSION_CHANGED가_먼저다() {
            //when, then
            assertThatThrownBy { memberService.createMember(guestSessionOf(ANONYMOUS_KEY_HASH), outdatedRequest()) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", POLICY_VERSION_CHANGED)
                .extracting { (it as RestApiException).exceptionRecovery?.kind }
                .isEqualTo(REFRESH_POLICIES)
        }
    }

    private fun guestSessionOf(anonymousKeyHash: String): AuthSession {
        val issuedTokenDto = authSessionIssuer.issue(
            sessionMode = GUEST,
            memberId = null,
            anonymousKeyHash = anonymousKeyHash
        )

        return authSessionService.authenticate(issuedTokenDto.accessToken)
    }

    private fun activeSessionOf(
        memberId: Long,
        anonymousKeyHash: String
    ): AuthSession {
        val issuedTokenDto = authSessionIssuer.issue(
            sessionMode = ACTIVE,
            memberId = memberId,
            anonymousKeyHash = anonymousKeyHash
        )

        return authSessionService.authenticate(issuedTokenDto.accessToken)
    }

    private fun validRequest(): CreateMemberRequest {
        return CreateMemberRequest(
            consents = listOf(
                ConsentRequest(
                    policyId = TERMS_OF_SERVICE_ID,
                    version = CURRENT_VERSION,
                    agreed = true
                ),
                ConsentRequest(
                    policyId = PRIVACY_POLICY_ID,
                    version = CURRENT_VERSION,
                    agreed = true
                )
            )
        )
    }

    private fun outdatedRequest(): CreateMemberRequest {
        return CreateMemberRequest(
            consents = listOf(
                ConsentRequest(
                    policyId = TERMS_OF_SERVICE_ID,
                    version = OUTDATED_VERSION,
                    agreed = true
                )
            )
        )
    }

    companion object {
        private const val ANONYMOUS_KEY_HASH = "anonymous-key-hash"
        private const val OTHER_ANONYMOUS_KEY_HASH = "other-anonymous-key-hash"
        private const val TERMS_OF_SERVICE_ID = "terms-of-service"
        private const val PRIVACY_POLICY_ID = "privacy-policy"
        private const val CURRENT_VERSION = "1"
        private const val OUTDATED_VERSION = "0"
        private const val TAKEN_MEMBER_CODE = "ARCA-AAAAA"
        private const val FREE_MEMBER_CODE = "ARCA-BBBBB"
        private const val MEMBER_CODE_PATTERN = "^ARCA-[A-Z0-9]{5}$"
    }
}
