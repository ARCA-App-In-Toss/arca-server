package com.arca.terms.service

import com.arca.global.exception.domain.ExceptionCode.POLICY_NOT_FOUND
import com.arca.global.exception.domain.RestApiException
import com.arca.terms.domain.Terms
import com.arca.terms.domain.TermsDocument
import com.arca.terms.dto.response.TermsDocumentResponse
import com.arca.terms.dto.response.TermsListResponse
import com.arca.terms.infra.TermsDocumentReader
import org.springframework.stereotype.Service

@Service
class TermsService(
    private val termsDocumentReader: TermsDocumentReader
) {

    fun getTerms(): TermsListResponse {
        return TermsListResponse.from(Terms.entries)
    }

    fun getTermsDocument(
        policyId: String,
        version: String
    ): TermsDocumentResponse {
        val termsDocument = TermsDocument.tryFrom(policyId, version)
            ?: throw RestApiException(POLICY_NOT_FOUND)

        return TermsDocumentResponse.of(
            termsDocument = termsDocument,
            content = termsDocumentReader.read(termsDocument)
        )
    }
}
