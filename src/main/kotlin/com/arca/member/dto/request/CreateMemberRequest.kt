package com.arca.member.dto.request

import com.arca.consent.dto.request.ConsentRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class CreateMemberRequest(
    @field:Schema(description = "동의 목록")
    @field:NotEmpty
    @field:Valid
    val consents: List<ConsentRequest>
)