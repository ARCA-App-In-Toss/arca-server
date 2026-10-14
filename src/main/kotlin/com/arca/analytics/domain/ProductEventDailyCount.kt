package com.arca.analytics.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType.IDENTITY
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "product_event_daily_count")
class ProductEventDailyCount private constructor(
    dateKst: LocalDate,
    productEventName: ProductEventName,
    totalCount: Long
) {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    var id: Long = 0L
        protected set

    @Column(nullable = false, name = "date_kst")
    var dateKst: LocalDate = dateKst
        protected set

    @Enumerated(STRING)
    @Column(nullable = false, name = "product_event_name")
    var productEventName: ProductEventName = productEventName
        protected set

    @Column(nullable = false, name = "total_count")
    var totalCount: Long = totalCount
        protected set

    companion object {
        fun create(
            dateKst: LocalDate,
            productEventName: ProductEventName,
            totalCount: Long
        ): ProductEventDailyCount {
            return ProductEventDailyCount(
                dateKst = dateKst,
                productEventName = productEventName,
                totalCount = totalCount
            )
        }
    }
}
