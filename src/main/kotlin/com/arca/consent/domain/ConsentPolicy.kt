package com.arca.consent.domain

enum class ConsentPolicy(
    val id: String,
    val version: String,
    val title: String,
    val url: String,
    val required: Boolean
) {
    TERMS_OF_SERVICE("terms-of-service", "1", "서비스 이용약관", "https://arca.invalid/policies/terms-of-service/1", true),
    PRIVACY_POLICY("privacy-policy", "1", "개인정보처리방침", "https://arca.invalid/policies/privacy-policy/1", true)
}