package com.arca.member.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import jakarta.validation.constraints.NotEmpty

data class UpdateNicknameRequest(
    @param:JsonProperty(required = true)
    @field:Schema(
        description = "닉네임. null이면 해제, 생략 불가",
        example = "달빛",
        nullable = true,
        requiredMode = REQUIRED
    )
    val nickname: String?,

    @field:Schema(
        description = "현재 profile revision",
        example = "1"
    )
    @field:NotEmpty
    val expectedRevision: String
)
