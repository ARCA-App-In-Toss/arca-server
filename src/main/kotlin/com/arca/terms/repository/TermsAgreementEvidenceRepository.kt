package com.arca.terms.repository

import com.arca.terms.domain.TermsAgreementEvidence
import org.springframework.data.jpa.repository.JpaRepository

interface TermsAgreementEvidenceRepository : JpaRepository<TermsAgreementEvidence, Long>
