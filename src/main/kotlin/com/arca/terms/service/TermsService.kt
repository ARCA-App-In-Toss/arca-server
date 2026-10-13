package com.arca.terms.service

import com.arca.terms.domain.Terms
import com.arca.terms.dto.response.TermsListResponse
import org.springframework.stereotype.Service

@Service
class TermsService {

    fun getTerms(): TermsListResponse {
        return TermsListResponse.from(Terms.entries)
    }
}
