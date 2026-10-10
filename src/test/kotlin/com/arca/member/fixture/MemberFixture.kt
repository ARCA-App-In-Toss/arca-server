package com.arca.member.fixture

import com.arca.member.domain.Member
import java.time.Instant

object MemberFixture {
    private const val MEMBER_CODE = "ARCA-TEST1"
    private val CREATED_AT = Instant.parse("2026-10-01T00:00:00Z")

    fun createMember(anonymousKeyHash: String): Member {
        return createMemberWithDetails(MEMBER_CODE, anonymousKeyHash)
    }

    fun createMemberWithDetails(
        memberCode: String,
        anonymousKeyHash: String
    ): Member {
        return Member.create(memberCode, anonymousKeyHash, CREATED_AT)
    }
}
