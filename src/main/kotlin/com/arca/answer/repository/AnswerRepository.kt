package com.arca.answer.repository

import com.arca.answer.domain.Answer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface AnswerRepository : JpaRepository<Answer, Long> {
    @Query("""
        SELECT a
        FROM Answer a
        JOIN FETCH a.semaphore
        JOIN FETCH a.question
        WHERE a.id = :answerId AND a.member.id = :memberId
    """)
    fun findByIdAndMemberId(
        @Param("answerId") answerId: Long,
        @Param("memberId") memberId: Long
    ): Answer?

    @Query("""
        SELECT a
        FROM Answer a
        JOIN FETCH a.semaphore
        JOIN FETCH a.question
        WHERE a.member.id = :memberId
          AND a.createdDateKst = :createdDateKst
    """)
    fun findByMemberIdAndCreatedDateKst(
        @Param("memberId") memberId: Long,
        @Param("createdDateKst") createdDateKst: LocalDate
    ): Answer?

    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM Answer a
        WHERE a.member.id = :memberId
          AND a.createdDateKst = :createdDateKst
    """)
    fun existsByMemberIdAndCreatedDateKst(
        @Param("memberId") memberId: Long,
        @Param("createdDateKst") createdDateKst: LocalDate
    ): Boolean

    @Query("""
        SELECT COUNT(a)
        FROM Answer a
        WHERE a.member.id = :memberId
    """)
    fun countByMemberId(
        @Param("memberId") memberId: Long
    ): Long
}