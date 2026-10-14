package com.arca.terms.domain

enum class Terms(
    val termsDocument: TermsDocument,
    val url: String,
    val required: Boolean
) {
    TERMS_OF_SERVICE(TermsDocument.TERMS_OF_SERVICE_V1, "https://arca.invalid/policies/terms-of-service/1", true),
    PRIVACY_POLICY(TermsDocument.PRIVACY_POLICY_V1, "https://arca.invalid/policies/privacy-policy/1", true);

    val id: String
        get() {
            return termsDocument.policyId
        }

    val version: String
        get() {
            return termsDocument.version
        }

    val title: String
        get() {
            return termsDocument.title
        }
}
