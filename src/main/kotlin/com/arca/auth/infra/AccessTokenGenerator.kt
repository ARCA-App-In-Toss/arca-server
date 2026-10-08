package com.arca.auth.infra

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64

@Component
class AccessTokenGenerator {

    fun generate(): String {
        val tokenBytes = ByteArray(TOKEN_BYTE_LENGTH)
        SECURE_RANDOM.nextBytes(tokenBytes)

        return BASE64_URL_ENCODER.encodeToString(tokenBytes)
    }

    companion object {
        private const val TOKEN_BYTE_LENGTH = 32
        private val SECURE_RANDOM = SecureRandom()
        private val BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding()
    }
}