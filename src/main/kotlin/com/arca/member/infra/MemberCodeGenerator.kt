package com.arca.member.infra

import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class MemberCodeGenerator {

    fun generate(): String {
        val randomPart = CharArray(RANDOM_LENGTH) {
            ALPHABET[SECURE_RANDOM.nextInt(ALPHABET.length)]
        }.concatToString()

        return "$PREFIX$randomPart"
    }

    companion object {
        private const val PREFIX = "ARCA-"
        private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private const val RANDOM_LENGTH = 5
        private val SECURE_RANDOM = SecureRandom()
    }
}