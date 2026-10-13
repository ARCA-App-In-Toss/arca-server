package com.arca.terms.repository

import com.arca.terms.domain.TermsAgreement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TermsAgreementRepository : JpaRepository<TermsAgreement, Long> {
    @Query("""
        SELECT t
        FROM TermsAgreement t
        WHERE t.member.id = :memberId
    """)
    fun findAllByMemberId(
        @Param("memberId") memberId: Long
    ): List<TermsAgreement>
}
