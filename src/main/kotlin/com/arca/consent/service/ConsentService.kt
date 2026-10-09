package com.arca.consent.service

import com.arca.consent.domain.ConsentPolicy
import com.arca.consent.dto.response.ConsentPoliciesResponse
import org.springframework.stereotype.Service

@Service
class ConsentService {

    fun getConsentPolicies(): ConsentPoliciesResponse {
        return ConsentPoliciesResponse.from(ConsentPolicy.entries)
    }
}
