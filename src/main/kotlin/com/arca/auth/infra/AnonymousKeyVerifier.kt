package com.arca.auth.infra

interface AnonymousKeyVerifier {
    fun verify(anonymousKey: String): Boolean
}