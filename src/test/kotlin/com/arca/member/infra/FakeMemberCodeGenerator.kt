package com.arca.member.infra

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class FakeMemberCodeGenerator : MemberCodeGenerator() {
    private val reservedMemberCodes = ArrayDeque<String>()

    fun reserve(vararg memberCodes: String) {
        reservedMemberCodes.addAll(memberCodes)
    }

    fun clear() {
        reservedMemberCodes.clear()
    }

    override fun generate(): String {
        return reservedMemberCodes.removeFirstOrNull() ?: super.generate()
    }
}
