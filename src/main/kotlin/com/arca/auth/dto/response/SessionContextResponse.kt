package com.arca.auth.dto.response

import com.arca.auth.domain.SessionMode
import com.arca.member.dto.response.MemberProfileResponse
import io.swagger.v3.oas.annotations.media.Schema

data class SessionContextResponse(
    @field:Schema(
        description = "세션 mode",
        example = "GUEST"
    )
    val mode: SessionMode,

    @field:Schema(
        description = "회원 profile. GUEST면 null",
        nullable = true
    )
    val member: MemberProfileResponse?
){
    companion object {
        fun of(
            sessionMode: SessionMode,
            member: MemberProfileResponse?
        ): SessionContextResponse {
            return SessionContextResponse(
                mode = sessionMode,
                member = member
            )
        }
    }
}
