package com.arca.analytics.service

import com.arca.analytics.domain.ProductEvent
import com.arca.analytics.domain.ProductEventName
import com.arca.analytics.dto.request.CreateEventBatchRequest
import com.arca.analytics.dto.request.ProductEventRequest
import com.arca.analytics.repository.ProductEventRepository
import com.arca.auth.domain.AuthSession
import com.arca.global.exception.domain.ExceptionCode.INVALID_REQUEST
import com.arca.global.exception.domain.RestApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class AnalyticsService(
    private val productEventRepository: ProductEventRepository,
    private val clock: Clock
) {
    @Transactional
    fun createEventBatch(
        authSession: AuthSession,
        request: CreateEventBatchRequest
    ) {
        val now = clock.instant()

        val productEvents = request.events
            .map {
                createProductEvent(
                    authSession = authSession,
                    request = request,
                    productEventRequest = it,
                    now = now
                )
            }
            .distinctBy { it.eventId }

        val savedEventIds = productEventRepository.findEventIdsByEventIdIn(productEvents.map { it.eventId }).toSet()

        productEventRepository.saveAll(productEvents.filterNot { it.eventId in savedEventIds })
    }

    private fun createProductEvent(
        authSession: AuthSession,
        request: CreateEventBatchRequest,
        productEventRequest: ProductEventRequest,
        now: Instant
    ): ProductEvent {
        if (productEventRequest.schemaVersion != ProductEvent.SUPPORTED_SCHEMA_VERSION) {
            throw RestApiException(INVALID_REQUEST)
        }

        val productEventName = ProductEventName.fromClientCode(productEventRequest.name)
        productEventName.validateProperties(productEventRequest.properties)

        return ProductEvent.create(
            eventId = productEventRequest.eventId,
            anonymousKeyHash = authSession.anonymousKeyHash,
            productEventName = productEventName,
            schemaVersion = productEventRequest.schemaVersion,
            properties = productEventRequest.properties,
            occurredAt = productEventRequest.occurredAt,
            receivedAt = now,
            appVersion = request.appVersion,
            productEventPlatform = request.platform
        )
    }
}
