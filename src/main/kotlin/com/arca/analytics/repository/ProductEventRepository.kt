package com.arca.analytics.repository

import com.arca.analytics.domain.ProductEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductEventRepository : JpaRepository<ProductEvent, Long> {
    @Query("""
        SELECT p.eventId
        FROM ProductEvent p
        WHERE p.eventId IN :eventIds
    """)
    fun findEventIdsByEventIdIn(
        @Param("eventIds") eventIds: Collection<String>
    ): List<String>

    @Modifying
    @Query("""
        DELETE FROM ProductEvent p
        WHERE p.anonymousKeyHash = :anonymousKeyHash
    """)
    fun deleteAllByAnonymousKeyHash(
        @Param("anonymousKeyHash") anonymousKeyHash: String
    )
}
