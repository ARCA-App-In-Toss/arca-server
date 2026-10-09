package com.arca.member.repository

import com.arca.member.domain.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MemberRepository : JpaRepository<Member, Long> {
    @Query("""
        SELECT m
        FROM Member m
        WHERE m.anonymousKeyHash = :anonymousKeyHash
    """)
    fun findByAnonymousKeyHash(
        @Param("anonymousKeyHash") anonymousKeyHash: String
    ): Member?

    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM Member m
        WHERE m.anonymousKeyHash = :anonymousKeyHash
    """)
    fun existsByAnonymousKeyHash(
        @Param("anonymousKeyHash") anonymousKeyHash: String
    ): Boolean

    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM Member m
        WHERE m.memberCode = :memberCode
    """)
    fun existsByMemberCode(
        @Param("memberCode") memberCode: String
    ): Boolean
}