package com.arca.consent.controller

import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.GUEST
import com.arca.consent.dto.response.ConsentPoliciesResponse
import com.arca.consent.service.ConsentService
import com.arca.global.annotation.AllowedSessionModes
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/consent-policies")
class ConsentController(
    private val consentService: ConsentService
) : ConsentControllerDocs {

    @AllowedSessionModes([GUEST, ACTIVE])
    @GetMapping
    override fun getConsentPolicies(): ResponseEntity<ConsentPoliciesResponse> {
        val response = consentService.getConsentPolicies()
        return ResponseEntity.status(OK).body(response)
    }
}
