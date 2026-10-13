package com.arca.answer.domain

import com.arca.global.exception.domain.ExceptionCode.ANSWER_CONTENT_INVALID
import com.arca.global.exception.domain.RestApiException
import com.arca.member.domain.Member
import com.arca.question.domain.Question
import com.arca.semaphore.domain.Semaphore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "answer")
class Answer private constructor(
    member: Member,
    semaphore: Semaphore,
    question: Question,
    content: String,
    createdDateKst: LocalDate,
    createdAt: Instant
) {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long = 0L
        protected set

    @ManyToOne(fetch = LAZY)
    @JoinColumn(nullable = false, name = "member_id")
    var member: Member = member
        protected set

    @ManyToOne(fetch = LAZY)
    @JoinColumn(nullable = false, name = "semaphore_id")
    var semaphore: Semaphore = semaphore
        protected set

    @ManyToOne(fetch = LAZY)
    @JoinColumn(nullable = false, name = "question_id")
    var question: Question = question
        protected set

    @Column(nullable = false, name = "content")
    var content: String = content
        protected set

    @Column(nullable = false, name = "created_date_kst")
    var createdDateKst: LocalDate = createdDateKst
        protected set

    @Column(nullable = false, name = "created_at")
    var createdAt: Instant = createdAt
        protected set

    var updatedAt: Instant = createdAt
        protected set

    fun isEdited(): Boolean {
        return updatedAt != createdAt
    }

    fun updateContent(
        content: String,
        updatedAt: Instant
    ) {
        validateContent(content)

        this.content = content
        this.updatedAt = updatedAt
    }

    companion object {
        private const val MIN_CONTENT_GRAPHEME_COUNT = 1
        private const val MAX_CONTENT_GRAPHEME_COUNT = 2000
        private const val MAX_CONTENT_CODE_POINT_COUNT = 15000
        private val GRAPHEME_CLUSTER_REGEX = Regex("\\X")

        fun create(
            member: Member,
            semaphore: Semaphore,
            question: Question,
            content: String,
            createdAt: Instant,
            createdDateKst: LocalDate
        ): Answer {
            validateContent(content)

            return Answer(
                member = member,
                semaphore = semaphore,
                question = question,
                content = content,
                createdDateKst = createdDateKst,
                createdAt = createdAt
            )
        }

        private fun validateContent(content: String) {
            if (content.codePointCount(0, content.length) > MAX_CONTENT_CODE_POINT_COUNT) {
                throw RestApiException(ANSWER_CONTENT_INVALID)
            }

            val graphemeCount = GRAPHEME_CLUSTER_REGEX.findAll(content).count()

            if (graphemeCount !in MIN_CONTENT_GRAPHEME_COUNT..MAX_CONTENT_GRAPHEME_COUNT) {
                throw RestApiException(ANSWER_CONTENT_INVALID)
            }
        }
    }
}