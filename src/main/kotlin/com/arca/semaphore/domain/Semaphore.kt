package com.arca.semaphore.domain

import com.arca.question.domain.Question
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "semaphore")
class Semaphore private constructor(
    code: String,
    dateKst: LocalDate?,
    primaryQuestion: Question,
    alternateQuestion: Question
) {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long = 0L
        protected set

    @Column(nullable = false, name = "code")
    var code: String = code
        protected set

    @Column(nullable = false, name = "version")
    var version: Long = INITIAL_CONTENT_VERSION
        protected set

    @Column(name = "date_kst")
    var dateKst: LocalDate? = dateKst
        protected set

    @ManyToOne(fetch = LAZY)
    @JoinColumn(nullable = false, name = "primary_question")
    var primaryQuestion: Question = primaryQuestion
        protected set

    @ManyToOne(fetch = LAZY)
    @JoinColumn(nullable = false, name = "alternate_question")
    var alternateQuestion: Question = alternateQuestion
        protected set

    companion object {
        private const val INITIAL_CONTENT_VERSION = 1L

        fun create(
            code: String,
            dateKst: LocalDate?,
            primaryQuestion: Question,
            alternateQuestion: Question
        ): Semaphore {
            return Semaphore(
                code = code,
                dateKst = dateKst,
                primaryQuestion = primaryQuestion,
                alternateQuestion = alternateQuestion
            )
        }
    }
}