package com.arca.member.dto.response

import com.arca.member.domain.Member
import io.swagger.v3.oas.annotations.media.Schema

data class MemberProfileResponse(
    @field:Schema(
        description = "표시용 회원 코드",
        example = "ARCA-7K2QX"
    )
    val memberCode: String,

    @field:Schema(
        description = "닉네임, 없으면 null",
        example = "달빛",
        nullable = true
    )
    val nickname: String?,

    @field:Schema(
        description = "profile revision",
        example = "1"
    )
    val revision: String
) {
    companion object {
        fun from(member: Member): MemberProfileResponse {
            return MemberProfileResponse(
                memberCode = member.memberCode,
                nickname = member.nickname,
                revision = member.revision.toString()
            )
        }
    }
}