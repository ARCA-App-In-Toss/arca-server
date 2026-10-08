package com.arca.global.annotation

import com.arca.auth.domain.SessionMode
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

@Target(FUNCTION)
@Retention(RUNTIME)
annotation class AllowedSessionModes(
    val sessionModes: Array<SessionMode>
)
