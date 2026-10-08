package com.arca.member.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class MemberProfileResponse(
    @field:Schema(
        description = "표시용 회원 코드",
        example = "SYNTHETIC-001"
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
        example = "r1"
    )
    val revision: String
)