package com.arca.question.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "question")
class Question private constructor(
    content: String
) {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long = 0L
        protected set

    @Column(nullable = false, name = "content")
    var content: String = content
        protected set

    fun hasId(questionId: String): Boolean {
        return id.toString() == questionId
    }

    companion object {
        fun create(content: String): Question {
            return Question(content = content)
        }
    }
}