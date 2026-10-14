package com.arca.terms.infra

import com.arca.terms.domain.TermsDocument
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class TermsDocumentReader {

    private val contents: Map<TermsDocument, String> = TermsDocument.entries.associateWith { readContent(it) }

    fun read(termsDocument: TermsDocument): String {
        return contents.getValue(termsDocument)
    }

    private fun readContent(termsDocument: TermsDocument): String {
        val content = ClassPathResource("$POLICY_DOCUMENT_DIRECTORY/${termsDocument.policyId}/${termsDocument.version}$POLICY_DOCUMENT_EXTENSION")
            .getContentAsString(Charsets.UTF_8)

        check(content.isNotBlank())

        return content
    }

    companion object {
        private const val POLICY_DOCUMENT_DIRECTORY = "policies"
        private const val POLICY_DOCUMENT_EXTENSION = ".md"
    }
}
