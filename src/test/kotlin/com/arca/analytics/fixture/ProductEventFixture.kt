package com.arca.analytics.fixture

import com.arca.analytics.domain.ProductEvent
import com.arca.analytics.domain.ProductEventName
import com.arca.analytics.domain.ProductEventName.ONBOARDING_STARTED
import java.time.Instant
import java.util.UUID

object ProductEventFixture {
    private const val SCHEMA_VERSION = 1
    private val OCCURRED_AT = Instant.parse("2026-10-01T00:00:00Z")
    private val RECEIVED_AT = Instant.parse("2026-10-01T00:00:01Z")

    fun createProductEvent(anonymousKeyHash: String): ProductEvent {
        return createProductEventWithDetails(anonymousKeyHash, ONBOARDING_STARTED)
    }

    fun createProductEventWithDetails(
        anonymousKeyHash: String,
        productEventName: ProductEventName
    ): ProductEvent {
        return ProductEvent.create(
            UUID.randomUUID(),
            anonymousKeyHash,
            productEventName,
            SCHEMA_VERSION,
            emptyMap(),
            OCCURRED_AT,
            RECEIVED_AT,
            null,
            null
        )
    }
}
