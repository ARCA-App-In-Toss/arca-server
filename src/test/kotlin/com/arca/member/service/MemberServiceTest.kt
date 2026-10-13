package com.arca.member.service

import com.arca.answer.fixture.AnswerFixture
import com.arca.answer.repository.AnswerRepository
import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.GUEST
import com.arca.auth.infra.AuthSessionIssuer
import com.arca.auth.repository.AuthSessionRepository
import com.arca.auth.service.AuthSessionService
import com.arca.global.exception.domain.ExceptionCode.MEMBER_ALREADY_EXISTS
import com.arca.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND
import com.arca.global.exception.domain.ExceptionCode.POLICY_VERSION_CHANGED
import com.arca.global.exception.domain.ExceptionRecoveryKind.REFRESH_POLICIES
import com.arca.global.exception.domain.RestApiException
import com.arca.global.infra.IntegrationTest
import com.arca.member.dto.request.CreateMemberRequest
import com.arca.member.fixture.MemberFixture
import com.arca.member.infra.FakeMemberCodeGenerator
import com.arca.member.repository.MemberRepository
import com.arca.question.fixture.QuestionFixture
import com.arca.semaphore.fixture.SemaphoreFixture
import com.arca.semaphore.repository.SemaphoreRepository
import com.arca.terms.domain.TermsAgreementEvidence
import com.arca.terms.dto.request.TermsAgreementRequest
import com.arca.terms.repository.TermsAgreementEvidenceRepository
import com.arca.terms.repository.TermsAgreementRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.MICROS

@IntegrationTest
class MemberServiceTest(
    private val memberService: MemberService,
    private val memberRepository: MemberRepository,
    private val answerRepository: AnswerRepository,
    private val authSessionRepository: AuthSessionRepository,
    private val semaphoreRepository: SemaphoreRepository,
    private val termsAgreementRepository: TermsAgreementRepository,
    private val termsAgreementEvidenceRepository: TermsAgreementEvidenceRepository,
    private val authSessionIssuer: AuthSessionIssuer,
    private val authSessionService: AuthSessionService,
    private val fakeMemberCodeGenerator: FakeMemberCodeGenerator,
    private val entityManager: EntityManager
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

    @Nested
    inner class 모든_데이터를_삭제할_때 {

        @Test
        fun 회원과_답변과_동의_기록을_지운다() {
            //given
            val activeSession = signUp(ANONYMOUS_KEY_HASH)
            val memberId = checkNotNull(activeSession.memberId)
            saveAnswersOf(memberId)
            flushAndClear()

            //when
            memberService.deleteMember(activeSession)
            flushAndClear()

            //then
            assertThat(memberRepository.findByIdOrNull(memberId)).isNull()
            assertThat(answerRepository.countByMemberId(memberId)).isZero()
            assertThat(termsAgreementRepository.findAllByMemberId(memberId)).isEmpty()
        }

        @Test
        fun 동의_기록마다_가명_주체값과_삭제_시각을_담은_증빙을_남긴다() {
            //given
            val activeSession = signUp(ANONYMOUS_KEY_HASH)
            flushAndClear()
            val termsAgreements = termsAgreementRepository.findAllByMemberId(checkNotNull(activeSession.memberId))
            val before = Instant.now().truncatedTo(MICROS)

            //when
            memberService.deleteMember(activeSession)
            flushAndClear()

            //then
            val after = Instant.now()
            val termsAgreementEvidences = termsAgreementEvidenceRepository.findAll()
            assertThat(termsAgreementEvidences)
                .extracting(
                    TermsAgreementEvidence::anonymousKeyHash,
                    TermsAgreementEvidence::termsId,
                    TermsAgreementEvidence::termsVersion,
                    TermsAgreementEvidence::agreedAt
                )
                .containsExactlyInAnyOrderElementsOf(
                    termsAgreements.map { tuple(ANONYMOUS_KEY_HASH, it.termsId, it.termsVersion, it.agreedAt) }
                )
            assertThat(termsAgreementEvidences).allSatisfy { assertThat(it.deletedAt).isBetween(before, after) }
        }

        @Test
        fun 그_회원의_유효한_세션을_모두_삭제_시각으로_폐기한다() {
            //given
            val activeSession = signUp(ANONYMOUS_KEY_HASH)
            val otherActiveSession = activeSessionOf(checkNotNull(activeSession.memberId), ANONYMOUS_KEY_HASH)
            flushAndClear()

            //when
            memberService.deleteMember(activeSession)
            flushAndClear()

            //then
            val deletedAt = termsAgreementEvidenceRepository.findAll().first().deletedAt
            assertThat(authSessionRepository.findAllById(listOf(activeSession.id, otherActiveSession.id)))
                .hasSize(2)
                .allSatisfy { assertThat(it.revokedAt).isEqualTo(deletedAt) }
        }

        @Test
        fun 이미_폐기된_세션의_폐기_시각은_바꾸지_않는다() {
            //given
            val activeSession = signUp(ANONYMOUS_KEY_HASH)
            val revokedSession = activeSessionOf(checkNotNull(activeSession.memberId), ANONYMOUS_KEY_HASH)
            authSessionService.revoke(revokedSession.id)
            flushAndClear()
            val revokedAt = authSessionRepository.findByIdOrNull(revokedSession.id)?.revokedAt

            //when
            memberService.deleteMember(activeSession)
            flushAndClear()

            //then
            assertThat(authSessionRepository.findByIdOrNull(revokedSession.id)?.revokedAt)
                .isNotNull()
                .isEqualTo(revokedAt)
        }

        @Test
        fun 다른_회원의_데이터는_그대로다() {
            //given
            val activeSession = signUp(ANONYMOUS_KEY_HASH)
            val otherActiveSession = signUp(OTHER_ANONYMOUS_KEY_HASH)
            val otherMemberId = checkNotNull(otherActiveSession.memberId)
            saveAnswersOf(checkNotNull(activeSession.memberId), otherMemberId)
            flushAndClear()

            //when
            memberService.deleteMember(activeSession)
            flushAndClear()

            //then
            assertThat(memberRepository.findByIdOrNull(otherMemberId)).isNotNull()
            assertThat(answerRepository.countByMemberId(otherMemberId)).isEqualTo(ANSWER_COUNT_PER_MEMBER)
            assertThat(termsAgreementRepository.findAllByMemberId(otherMemberId)).hasSize(CONSENT_POLICY_COUNT)
            assertThat(authSessionRepository.findByIdOrNull(otherActiveSession.id)?.revokedAt).isNull()
            assertThat(termsAgreementEvidenceRepository.findAll()).hasSize(CONSENT_POLICY_COUNT)
        }

        @Test
        fun 같은_익명_키로_다시_가입하면_삭제_전_데이터와_합치지_않은_새_회원이_생긴다() {
            //given
            val activeSession = signUp(ANONYMOUS_KEY_HASH)
            val memberId = checkNotNull(activeSession.memberId)
            saveAnswersOf(memberId)
            flushAndClear()
            memberService.deleteMember(activeSession)
            flushAndClear()

            //when
            val newActiveSession = signUp(ANONYMOUS_KEY_HASH)

            //then
            val newMemberId = checkNotNull(newActiveSession.memberId)
            assertThat(newMemberId).isNotEqualTo(memberId)
            assertThat(answerRepository.countByMemberId(newMemberId)).isZero()
        }

        @Test
        fun GUEST_세션이면_MEMBER_NOT_FOUND() {
            //when, then
            assertThatThrownBy { memberService.deleteMember(guestSessionOf(ANONYMOUS_KEY_HASH)) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND)
        }

        @Test
        fun 회원이_이미_지워졌으면_MEMBER_NOT_FOUND() {
            //given
            val activeSession = signUp(ANONYMOUS_KEY_HASH)
            memberService.deleteMember(activeSession)
            flushAndClear()

            //when, then
            assertThatThrownBy { memberService.deleteMember(activeSession) }
                .isInstanceOf(RestApiException::class.java)
                .hasFieldOrPropertyWithValue("exceptionCode", MEMBER_NOT_FOUND)
        }
    }

    private fun signUp(anonymousKeyHash: String): AuthSession {
        val sessionResponse = memberService.createMember(guestSessionOf(anonymousKeyHash), validRequest())
        return authSessionService.authenticate(sessionResponse.accessToken)
    }

    private fun saveAnswersOf(vararg memberIds: Long) {
        val primaryQuestion = QuestionFixture.createQuestion()
        val alternateQuestion = QuestionFixture.createQuestion()
        entityManager.persist(primaryQuestion)
        entityManager.persist(alternateQuestion)
        val semaphore = semaphoreRepository.save(SemaphoreFixture.createSemaphore(SEMAPHORE_DATE, primaryQuestion, alternateQuestion))

        memberIds.forEach { memberId ->
            val member = checkNotNull(memberRepository.findByIdOrNull(memberId))
            ANSWER_DATES.forEach { answerRepository.save(AnswerFixture.createAnswer(member, semaphore, it)) }
        }
    }

    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
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
                TermsAgreementRequest(
                    policyId = TERMS_OF_SERVICE_ID,
                    version = CURRENT_VERSION,
                    agreed = true
                ),
                TermsAgreementRequest(
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
                TermsAgreementRequest(
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
        private const val CONSENT_POLICY_COUNT = 2
        private const val ANSWER_COUNT_PER_MEMBER = 2L
        private val SEMAPHORE_DATE = LocalDate.parse("2026-10-01")
        private val ANSWER_DATES = listOf(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-02"))
    }
}
