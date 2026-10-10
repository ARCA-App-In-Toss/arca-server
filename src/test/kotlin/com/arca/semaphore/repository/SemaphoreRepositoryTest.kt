package com.arca.semaphore.repository

import com.arca.global.infra.IntegrationTest
import com.arca.question.domain.Question
import com.arca.question.fixture.QuestionFixture
import com.arca.semaphore.fixture.SemaphoreFixture
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate

@IntegrationTest
class SemaphoreRepositoryTest(
    private val semaphoreRepository: SemaphoreRepository,

    private val entityManager: EntityManager
) {

    @Nested
    inner class 날짜_편성_제약 {

        @Test
        fun 같은_날짜에_세마포어를_둘_편성할_수_없다() {
            //given
            semaphoreRepository.saveAndFlush(SemaphoreFixture.createSemaphore(DATE_KST, saveQuestion(), saveQuestion()))

            //when & then
            assertThatThrownBy {
                semaphoreRepository.saveAndFlush(SemaphoreFixture.createSemaphore(DATE_KST, saveQuestion(), saveQuestion()))
            }.isInstanceOf(DataIntegrityViolationException::class.java)
        }

        @Test
        fun 날짜가_없는_예비_세마포어는_여럿_둘_수_있다() {
            //given
            semaphoreRepository.saveAndFlush(SemaphoreFixture.createSemaphore(null, saveQuestion(), saveQuestion()))

            //when
            semaphoreRepository.saveAndFlush(SemaphoreFixture.createSemaphore(null, saveQuestion(), saveQuestion()))

            //then
            assertThat(semaphoreRepository.count()).isEqualTo(RESERVE_COUNT)
        }
    }

    @Nested
    inner class 날짜로_조회할_때 {

        @Test
        fun 그_날짜에_편성된_세마포어와_두_질문을_함께_읽는다() {
            //given
            val primaryQuestion = saveQuestion()
            val alternateQuestion = saveQuestion()
            semaphoreRepository.saveAndFlush(SemaphoreFixture.createSemaphore(DATE_KST, primaryQuestion, alternateQuestion))
            entityManager.clear()

            //when
            val semaphore = checkNotNull(semaphoreRepository.findByDateKst(DATE_KST))

            //then
            assertThat(semaphore.primaryQuestion.id).isEqualTo(primaryQuestion.id)
            assertThat(semaphore.alternateQuestion.id).isEqualTo(alternateQuestion.id)
            assertThat(entityManager.entityManagerFactory.persistenceUnitUtil.isLoaded(semaphore.primaryQuestion)).isTrue()
            assertThat(entityManager.entityManagerFactory.persistenceUnitUtil.isLoaded(semaphore.alternateQuestion)).isTrue()
        }

        @Test
        fun 편성되지_않은_날짜면_null이다() {
            //given
            semaphoreRepository.saveAndFlush(SemaphoreFixture.createSemaphore(DATE_KST, saveQuestion(), saveQuestion()))

            //when
            val semaphore = semaphoreRepository.findByDateKst(DATE_KST.plusDays(1))

            //then
            assertThat(semaphore).isNull()
        }
    }

    private fun saveQuestion(): Question {
        val question = QuestionFixture.createQuestion()
        entityManager.persist(question)
        return question
    }

    companion object {
        private const val RESERVE_COUNT = 2L
        private val DATE_KST = LocalDate.of(2026, 10, 10)
    }
}
