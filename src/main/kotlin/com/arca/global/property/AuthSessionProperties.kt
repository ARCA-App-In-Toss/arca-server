package com.arca.global.property

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "auth-session")
data class AuthSessionProperties(
    val ttl: Duration
)