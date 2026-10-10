package com.arca.semaphore.repository

import com.arca.semaphore.domain.Semaphore
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface SemaphoreRepository: JpaRepository<Semaphore, Long> {
    @Query("""
        SELECT s
        FROM Semaphore s
        JOIN FETCH s.primaryQuestion
        JOIN FETCH s.alternateQuestion
        WHERE s.dateKst = :dateKst
    """)
    fun findByDateKst(
        @Param("dateKst") dateKst: LocalDate
    ): Semaphore?
}