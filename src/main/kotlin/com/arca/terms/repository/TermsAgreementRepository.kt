package com.arca.terms.repository

import com.arca.terms.domain.TermsAgreement
import org.springframework.data.jpa.repository.JpaRepository

interface TermsAgreementRepository : JpaRepository<TermsAgreement, Long>