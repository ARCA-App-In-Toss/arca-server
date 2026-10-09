package com.arca.member.domain

import com.arca.global.exception.domain.ExceptionCode.NICKNAME_INVALID
import com.arca.global.exception.domain.RestApiException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "member")
class Member private constructor(
    memberCode: String,
    anonymousKeyHash: String,
    createdAt: Instant
) {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long = 0L
        protected set

    @Column(nullable = false, name = "member_code")
    var memberCode: String = memberCode
        protected set

    @Column(nullable = false, name = "anonymous_key_hash")
    var anonymousKeyHash: String = anonymousKeyHash
        protected set

    @Column(name = "nickname")
    var nickname: String? = null
        protected set

    @Column(nullable = false, name = "revision")
    var revision: Long = 0L
        protected set

    @Column(nullable = false, name = "created_at")
    var createdAt: Instant = createdAt
        protected set

    fun hasRevision(revision: String): Boolean {
        return this.revision.toString() == revision
    }

    fun updateNickname(nickname: String?) {
        this.nickname = nickname?.let { normalizeNickname(it) }
        revision++
    }

    private fun normalizeNickname(nickname: String): String {
        val trimmedNickname = nickname.trim()

        if (trimmedNickname.length > MAX_NICKNAME_LENGTH) {
            throw RestApiException(NICKNAME_INVALID)
        }

        if (trimmedNickname.codePoints().toArray().any { isForbiddenCodePoint(it) }) {
            throw RestApiException(NICKNAME_INVALID)
        }

        val graphemeCount = GRAPHEME_CLUSTER_REGEX.findAll(trimmedNickname).count()

        if (graphemeCount !in MIN_NICKNAME_GRAPHEME_COUNT..MAX_NICKNAME_GRAPHEME_COUNT) {
            throw RestApiException(NICKNAME_INVALID)
        }

        return trimmedNickname
    }

    private fun isForbiddenCodePoint(codePoint: Int): Boolean {
        return when {
            codePoint in INVISIBLE_FILLER_CODE_POINTS -> true
            codePoint == ZERO_WIDTH_JOINER || codePoint in TAG_CODE_POINT_RANGE -> false
            else -> Character.getType(codePoint).toByte() in FORBIDDEN_CHARACTER_TYPES
        }
    }

    companion object {
        private const val MAX_NICKNAME_LENGTH = 255
        private const val MIN_NICKNAME_GRAPHEME_COUNT = 2
        private const val MAX_NICKNAME_GRAPHEME_COUNT = 12
        private const val ZERO_WIDTH_JOINER = 0x200D
        private val GRAPHEME_CLUSTER_REGEX = Regex("\\X")
        private val TAG_CODE_POINT_RANGE = 0xE0020..0xE007F
        private val INVISIBLE_FILLER_CODE_POINTS = setOf(0x115F, 0x1160, 0x3164, 0xFFA0, 0x2800)
        private val FORBIDDEN_CHARACTER_TYPES = setOf(
            Character.CONTROL,
            Character.LINE_SEPARATOR,
            Character.PARAGRAPH_SEPARATOR,
            Character.SURROGATE,
            Character.FORMAT
        )

        fun create(
            memberCode: String,
            anonymousKeyHash: String,
            createdAt: Instant
        ): Member {
            return Member(
                memberCode = memberCode,
                anonymousKeyHash = anonymousKeyHash,
                createdAt = createdAt
            )
        }
    }
}