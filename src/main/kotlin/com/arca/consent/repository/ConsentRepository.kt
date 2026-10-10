package com.arca.consent.repository

import com.arca.consent.domain.Consent
import org.springframework.data.jpa.repository.JpaRepository

interface ConsentRepository : JpaRepository<Consent, Long>