package com.arca.auth.dto.response

import com.arca.auth.domain.SessionMode
import com.arca.passenger.dto.response.PassengerProfileResponse
import io.swagger.v3.oas.annotations.media.Schema

data class SessionContextResponse(
    @field:Schema(
        description = "세션 mode",
        example = "PRE_PASSENGER"
    )
    val mode: SessionMode,

    @field:Schema(
        description = "승객 profile. PRE_PASSENGER면 null",
        nullable = true
    )
    val passenger: PassengerProfileResponse?
){
    companion object {
        fun of(
            sessionMode: SessionMode,
            passenger: PassengerProfileResponse?
        ): SessionContextResponse {
            return SessionContextResponse(
                mode = sessionMode,
                passenger = passenger
            )
        }
    }
}
