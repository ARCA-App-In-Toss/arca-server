package com.arca.member.service

import com.arca.auth.domain.AuthSession
import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.dto.response.SessionContextResponse
import com.arca.auth.dto.response.SessionResponse
import com.arca.auth.infra.AuthSessionIssuer
import com.arca.auth.repository.AuthSessionRepository
import com.arca.global.exception.domain.ExceptionCode.MEMBER_ALREADY_EXISTS
import com.arca.global.exception.domain.ExceptionCode.MEMBER_NOT_FOUND
import com.arca.global.exception.domain.ExceptionCode.SESSION_INVALID
import com.arca.global.exception.domain.RestApiException
import com.arca.member.domain.Member
import com.arca.member.dto.request.CreateMemberRequest
import com.arca.member.dto.request.UpdateNicknameRequest
import com.arca.member.dto.response.MemberProfileResponse
import com.arca.member.infra.MemberCodeGenerator
import com.arca.member.repository.MemberRepository
import com.arca.terms.domain.Terms
import com.arca.terms.domain.TermsAgreement
import com.arca.terms.infra.TermsAgreementValidator
import com.arca.terms.repository.TermsAgreementRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val authSessionRepository: AuthSessionRepository,
    private val termsAgreementRepository: TermsAgreementRepository,
    private val authSessionIssuer: AuthSessionIssuer,
    private val memberCodeGenerator: MemberCodeGenerator,
    private val termsAgreementValidator: TermsAgreementValidator,
    private val clock: Clock
) {
    @Transactional(readOnly = true)
    fun getMyMember(authSession: AuthSession): MemberProfileResponse {
        val member = findMember(authSession)
        return MemberProfileResponse.from(member)
    }

    @Transactional
    fun createMember(
        authSession: AuthSession,
        request: CreateMemberRequest
    ): SessionResponse {
        if (authSession.sessionMode == ACTIVE || memberRepository.existsByAnonymousKeyHash(authSession.anonymousKeyHash)) {
            throw RestApiException(MEMBER_ALREADY_EXISTS)
        }

        termsAgreementValidator.validateTermsAgreement(request.consents)

        val now = clock.instant()
        val member = saveMember(
            anonymousKeyHash = authSession.anonymousKeyHash,
            createdAt = now
        )

        termsAgreementRepository.saveAll(
            Terms.entries.map {
                TermsAgreement.create(
                    member = member,
                    terms = it,
                    agreedAt = now
                )
            }
        )

        val guestAuthSession = authSessionRepository.findByIdOrNull(authSession.id)
            ?: throw RestApiException(SESSION_INVALID)

        guestAuthSession.revoke(now)

        val issuedTokenDto = authSessionIssuer.issue(
            sessionMode = ACTIVE,
            memberId = member.id,
            anonymousKeyHash = member.anonymousKeyHash
        )

        return SessionResponse.of(
            issuedTokenDto = issuedTokenDto,
            context = SessionContextResponse.of(
                sessionMode = ACTIVE,
                member = MemberProfileResponse.from(member)
            )
        )
    }

    @Transactional
    fun updateNickname(
        authSession: AuthSession,
        request: UpdateNicknameRequest
    ): MemberProfileResponse {
        val member = findMember(authSession)

        member.updateNickname(request.nickname)

        return MemberProfileResponse.from(member)
    }

    private fun findMember(authSession: AuthSession): Member {
        val memberId = authSession.memberId
            ?: throw RestApiException(MEMBER_NOT_FOUND)

        return memberRepository.findByIdOrNull(memberId)
            ?: throw RestApiException(MEMBER_NOT_FOUND)
    }

    private fun saveMember(
        anonymousKeyHash: String,
        createdAt: Instant
    ): Member {
        val memberCode = generateSequence { memberCodeGenerator.generate() }
            .first { !memberRepository.existsByMemberCode(it) }

        val member = Member.create(
            memberCode = memberCode,
            anonymousKeyHash = anonymousKeyHash,
            createdAt = createdAt
        )

        return memberRepository.save(member)
    }
}