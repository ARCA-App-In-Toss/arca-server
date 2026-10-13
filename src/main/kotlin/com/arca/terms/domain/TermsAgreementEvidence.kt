package com.arca.terms.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "terms_agreement_evidence")
class TermsAgreementEvidence private constructor(
    anonymousKeyHash: String,
    termsId: String,
    termsVersion: String,
    agreedAt: Instant,
    deletedAt: Instant
) {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long = 0L
        protected set

    @Column(nullable = false, name = "anonymous_key_hash")
    var anonymousKeyHash: String = anonymousKeyHash
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

    @Column(nullable = false, name = "deleted_at")
    var deletedAt: Instant = deletedAt
        protected set

    companion object {
        fun create(
            anonymousKeyHash: String,
            termsId: String,
            termsVersion: String,
            agreedAt: Instant,
            deletedAt: Instant
        ): TermsAgreementEvidence {
            return TermsAgreementEvidence(
                anonymousKeyHash = anonymousKeyHash,
                termsId = termsId,
                termsVersion = termsVersion,
                agreedAt = agreedAt,
                deletedAt = deletedAt
            )
        }
    }
}
