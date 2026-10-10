package com.arca.consent.domain

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
@Table(name = "consent")
class Consent private constructor(
    member: Member,
    policyId: String,
    policyVersion: String,
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

    @Column(nullable = false, name = "policy_id")
    var policyId: String = policyId
        protected set

    @Column(nullable = false, name = "policy_version")
    var policyVersion: String = policyVersion
        protected set

    @Column(nullable = false, name = "agreed_at")
    var agreedAt: Instant = agreedAt
        protected set

    companion object {
        fun create(
            member: Member,
            consentPolicy: ConsentPolicy,
            agreedAt: Instant
        ): Consent {
            return Consent(
                member = member,
                policyId = consentPolicy.id,
                policyVersion = consentPolicy.version,
                agreedAt = agreedAt
            )
        }
    }
}