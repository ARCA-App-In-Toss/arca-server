package com.arca.question.domain

import jakarta.persistence.*
import jakarta.persistence.GenerationType.IDENTITY

@Entity
@Table(name = "question")
class Question private constructor(
    content: String
){
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long = 0L
        protected set

    @Column(nullable = false, name = "content")
    var content: String = content
        protected set

    @Column(nullable = false, name = "content_version")
    var contentVersion: Long = INITIAL_CONTENT_VERSION
        protected set

    companion object{
        private const val INITIAL_CONTENT_VERSION = 1L

        fun create(content: String): Question{
            return Question(content = content)
        }
    }
}