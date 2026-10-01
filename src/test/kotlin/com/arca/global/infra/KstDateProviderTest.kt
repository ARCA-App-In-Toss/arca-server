package com.arca.global.infra

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC

@IntegrationTest
class KstDateProviderTest(
    private val kstDateProvider: KstDateProvider,

    private val clock: Clock
) {

    @Nested
    inner class 오늘_날짜를_구할_때 {

        @Test
        fun KST_자정_직전이면_전날_날짜다() {
            //given
            val provider = KstDateProvider(Clock.fixed(Instant.parse(BEFORE_KST_MIDNIGHT), UTC))

            //when
            val today = provider.today()

            //then
            assertThat(today).isEqualTo(LocalDate.parse(KST_DATE))
        }

        @Test
        fun KST_자정이_되면_다음_날짜로_넘어간다() {
            //given
            val provider = KstDateProvider(Clock.fixed(Instant.parse(KST_MIDNIGHT), UTC))

            //when
            val today = provider.today()

            //then
            assertThat(today).isEqualTo(LocalDate.parse(NEXT_KST_DATE))
        }

        @Test
        fun 주입된_Clock의_현재_시각을_기준으로_한다() {
            //given
            val fixedClock = Clock.fixed(clock.instant(), UTC)
            val provider = KstDateProvider(fixedClock)

            //when
            val today = provider.today()

            //then
            assertThat(today).isEqualTo(kstDateProvider.dateOf(fixedClock.instant()))
        }
    }

    @Nested
    inner class 시각을_날짜로_바꿀_때 {

        @Test
        fun KST_자정_직전_시각은_전날_날짜다() {
            //when
            val date = kstDateProvider.dateOf(Instant.parse(BEFORE_KST_MIDNIGHT))

            //then
            assertThat(date).isEqualTo(LocalDate.parse(KST_DATE))
        }

        @Test
        fun KST_자정_시각은_다음_날짜다() {
            //when
            val date = kstDateProvider.dateOf(Instant.parse(KST_MIDNIGHT))

            //then
            assertThat(date).isEqualTo(LocalDate.parse(NEXT_KST_DATE))
        }
    }

    @Test
    fun 등록된_Clock_빈은_UTC_기준이다() {
        //then
        assertThat(clock.zone).isEqualTo(UTC)
    }

    companion object {
        private const val BEFORE_KST_MIDNIGHT = "2026-09-13T14:59:59Z"
        private const val KST_MIDNIGHT = "2026-09-13T15:00:00Z"
        private const val KST_DATE = "2026-09-13"
        private const val NEXT_KST_DATE = "2026-09-14"
    }
}
