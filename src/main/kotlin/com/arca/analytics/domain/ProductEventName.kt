package com.arca.analytics.domain

import com.arca.analytics.domain.ProductEventOwner.CLIENT
import com.arca.analytics.domain.ProductEventOwner.SERVER
import com.arca.global.exception.domain.ExceptionCode.INVALID_REQUEST
import com.arca.global.exception.domain.RestApiException

enum class ProductEventName(
    val code: String,
    val productEventOwner: ProductEventOwner,
    val allowedProperties: Map<String, Set<String>>
) {
    ONBOARDING_STARTED("onboarding_started", CLIENT, emptyMap()),
    ONBOARDING_SKIPPED("onboarding_skipped", CLIENT, emptyMap()),
    TODAY_SEMA_VIEWED(
        "today_sema_viewed",
        CLIENT,
        mapOf("answerState" to setOf("UNANSWERED", "ANSWERED"))
    ),
    ALTERNATE_QUESTION_VIEWED("alternate_question_viewed", CLIENT, emptyMap()),
    SEMA_QUESTION_CHANGED(
        "sema_question_changed",
        CLIENT,
        mapOf(
            "from" to setOf("PRIMARY", "ALTERNATE"),
            "to" to setOf("PRIMARY", "ALTERNATE")
        )
    ),
    ANSWER_STARTED(
        "answer_started",
        CLIENT,
        mapOf("mode" to setOf("CREATE", "UPDATE"))
    ),
    ANSWER_SAVE_FAILED(
        "answer_save_failed",
        CLIENT,
        mapOf(
            "mode" to setOf("CREATE", "UPDATE"),
            "certainty" to setOf("REJECTED", "UNKNOWN"),
            "reasonCode" to setOf(
                "VALIDATION_REJECTED",
                "DATE_CHANGED",
                "CONTENT_REPLACED",
                "CONFLICT",
                "NETWORK_UNCONFIRMED",
                "SERVER_UNAVAILABLE",
                "PROTOCOL_UNCONFIRMED"
            )
        )
    ),
    ARCHIVE_VIEWED(
        "archive_viewed",
        CLIENT,
        mapOf("state" to setOf("EMPTY", "NON_EMPTY"))
    ),
    ONBOARDING_COMPLETED("onboarding_completed", SERVER, emptyMap()),
    ANSWER_SAVED("answer_saved", SERVER, emptyMap()),
    ANSWER_EDITED("answer_edited", SERVER, emptyMap()),
    ANSWER_DELETED("answer_deleted", SERVER, emptyMap()),
    ALL_DATA_DELETED("all_data_deleted", SERVER, emptyMap());

    fun validateProperties(properties: Map<String, String>) {
        val hasInvalidProperty = properties.keys != allowedProperties.keys ||
            properties.any { (key, value) -> value !in allowedProperties.getValue(key) }

        if (hasInvalidProperty) {
            throw RestApiException(INVALID_REQUEST)
        }
    }

    companion object {
        fun fromClientCode(code: String): ProductEventName {
            return entries.firstOrNull { it.code == code && it.productEventOwner == CLIENT }
                ?: throw RestApiException(INVALID_REQUEST)
        }
    }
}
