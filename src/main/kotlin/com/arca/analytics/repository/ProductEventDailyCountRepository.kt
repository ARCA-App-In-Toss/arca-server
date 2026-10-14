package com.arca.analytics.repository

import com.arca.analytics.domain.ProductEventDailyCount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ProductEventDailyCountRepository : JpaRepository<ProductEventDailyCount, Long> {
    @Modifying
    @Query(
        value = """
            INSERT INTO product_event_daily_count (date_kst, product_event_name, total_count)
            VALUES (:dateKst, :productEventName, 1)
            ON DUPLICATE KEY UPDATE total_count = total_count + 1
        """,
        nativeQuery = true
    )
    fun increaseTotalCount(
        @Param("dateKst") dateKst: LocalDate,
        @Param("productEventName") productEventName: String
    )
}
