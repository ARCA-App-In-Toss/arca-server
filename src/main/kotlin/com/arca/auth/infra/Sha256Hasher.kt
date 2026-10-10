package com.arca.auth.infra

import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.HexFormat
import kotlin.text.Charsets.UTF_8

@Component
class Sha256Hasher {

    fun hash(value: String): String {
        val messageDigest = MessageDigest.getInstance(HASH_ALGORITHM)
        val digest = messageDigest.digest(value.toByteArray(UTF_8))

        return HEX_FORMAT.formatHex(digest)
    }

    companion object {
        private const val HASH_ALGORITHM = "SHA-256"
        private val HEX_FORMAT = HexFormat.of()
    }
}