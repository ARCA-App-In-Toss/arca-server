package com.arca.terms.controller

import com.arca.auth.domain.SessionMode.ACTIVE
import com.arca.auth.domain.SessionMode.GUEST
import com.arca.global.annotation.AllowedSessionModes
import com.arca.terms.dto.response.TermsDocumentResponse
import com.arca.terms.dto.response.TermsListResponse
import com.arca.terms.service.TermsService
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/consent-policies")
class TermsController(
    private val termsService: TermsService
) : TermsControllerDocs {

    @AllowedSessionModes([GUEST, ACTIVE])
    @GetMapping
    override fun getTerms(): ResponseEntity<TermsListResponse> {
        val response = termsService.getTerms()
        return ResponseEntity.status(OK).body(response)
    }

    @AllowedSessionModes([GUEST, ACTIVE])
    @GetMapping("/{policyId}/versions/{version}")
    override fun getTermsDocument(
        @PathVariable policyId: String,
        @PathVariable version: String
    ): ResponseEntity<TermsDocumentResponse> {
        val response = termsService.getTermsDocument(policyId, version)
        return ResponseEntity.status(OK).body(response)
    }
}
