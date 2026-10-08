package com.arca.auth.dto.internal

import java.time.Instant

data class IssuedTokenDto(
    val accessToken: String,
    val expiresAt: Instant
)
