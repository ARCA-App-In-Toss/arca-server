package com.arca.global.infra

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Component
class KstDateProvider(
    private val clock: Clock
) {

    fun today(): LocalDate {
        return LocalDate.now(clock.withZone(KST))
    }

    fun dateOf(instant: Instant): LocalDate {
        return instant.atZone(KST).toLocalDate()
    }

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
    }
}
