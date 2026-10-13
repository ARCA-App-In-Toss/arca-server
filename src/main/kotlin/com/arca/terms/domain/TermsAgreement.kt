package com.arca.terms.domain

import com.arca.member.domain.Member
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "terms_agreement")
class TermsAgreement private constructor(
    member: Member,
    termsId: String,
    termsVersion: String,
    agreedAt: Instant
) {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long = 0L
        protected set

    @ManyToOne(fetch = LAZY)
    @JoinColumn(nullable = false, name = "member_id")
    var member: Member = member
        protected set

    @Column(nullable = false, name = "terms_id")
    var termsId: String = termsId
        protected set

    @Column(nullable = false, name = "terms_version")
    var termsVersion: String = termsVersion
        protected set

    @Column(nullable = false, name = "agreed_at")
    var agreedAt: Instant = agreedAt
        protected set

    companion object {
        fun create(
            member: Member,
            terms: Terms,
            agreedAt: Instant
        ): TermsAgreement {
            return TermsAgreement(
                member = member,
                termsId = terms.id,
                termsVersion = terms.version,
                agreedAt = agreedAt
            )
        }
    }
}