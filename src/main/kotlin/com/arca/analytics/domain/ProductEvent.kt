package com.arca.analytics.domain

import com.arca.analytics.infra.ProductEventPropertiesConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product_event")
class ProductEvent private constructor(
    eventId: String,
    anonymousKeyHash: String,
    productEventName: ProductEventName,
    schemaVersion: Int,
    properties: Map<String, String>,
    occurredAt: Instant,
    receivedAt: Instant,
    appVersion: String?,
    productEventPlatform: ProductEventPlatform?
) {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long = 0L
        protected set

    @Column(nullable = false, name = "event_id")
    var eventId: String = eventId
        protected set

    @Column(nullable = false, name = "anonymous_key_hash")
    var anonymousKeyHash: String = anonymousKeyHash
        protected set

    @Enumerated(STRING)
    @Column(nullable = false, name = "product_event_name")
    var productEventName: ProductEventName = productEventName
        protected set

    @Column(nullable = false, name = "schema_version")
    var schemaVersion: Int = schemaVersion
        protected set

    @Convert(converter = ProductEventPropertiesConverter::class)
    @Column(nullable = false, name = "properties")
    var properties: Map<String, String> = properties
        protected set

    @Column(nullable = false, name = "occurred_at")
    var occurredAt: Instant = occurredAt
        protected set

    @Column(nullable = false, name = "received_at")
    var receivedAt: Instant = receivedAt
        protected set

    @Column(name = "app_version")
    var appVersion: String? = appVersion
        protected set

    @Enumerated(STRING)
    @Column(name = "product_event_platform")
    var productEventPlatform: ProductEventPlatform? = productEventPlatform
        protected set

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1

        fun create(
            eventId: UUID,
            anonymousKeyHash: String,
            productEventName: ProductEventName,
            schemaVersion: Int,
            properties: Map<String, String>,
            occurredAt: Instant,
            receivedAt: Instant,
            appVersion: String?,
            productEventPlatform: ProductEventPlatform?
        ): ProductEvent {
            return ProductEvent(
                eventId = eventId.toString(),
                anonymousKeyHash = anonymousKeyHash,
                productEventName = productEventName,
                schemaVersion = schemaVersion,
                properties = properties,
                occurredAt = occurredAt,
                receivedAt = receivedAt,
                appVersion = appVersion,
                productEventPlatform = productEventPlatform
            )
        }

        fun createServerEvent(
            anonymousKeyHash: String,
            productEventName: ProductEventName,
            now: Instant
        ): ProductEvent {
            return create(
                eventId = UUID.randomUUID(),
                anonymousKeyHash = anonymousKeyHash,
                productEventName = productEventName,
                schemaVersion = SUPPORTED_SCHEMA_VERSION,
                properties = emptyMap(),
                occurredAt = now,
                receivedAt = now,
                appVersion = null,
                productEventPlatform = null
            )
        }
    }
}
