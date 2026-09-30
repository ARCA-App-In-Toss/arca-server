package com.arca.global.exception.domain

import com.arca.global.exception.domain.ExceptionCode.ANSWER_NOT_FOUND
import com.arca.global.exception.domain.ExceptionCode.COMMAND_ALREADY_PENDING
import com.arca.global.exception.domain.ExceptionCode.INTERNAL_ERROR
import com.arca.global.exception.domain.ExceptionCode.MAINTENANCE
import com.arca.global.exception.domain.ExceptionCode.RATE_LIMITED
import com.arca.global.exception.domain.ExceptionCode.SESSION_RECOVERY_REQUIRED
import com.arca.global.infra.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@IntegrationTest
class RestApiExceptionTest {

    @Nested
    inner class recovery를_검사할_때 {

        @Test
        fun recovery가_없는_code는_code만으로_생성된다() {
            //when
            val restApiException = RestApiException(ANSWER_NOT_FOUND)

            //then
            assertThat(restApiException.exceptionCode).isEqualTo(ANSWER_NOT_FOUND)
            assertThat(restApiException.exceptionRecovery).isNull()
        }

        @Test
        fun 계약이_정한_kind의_recovery를_실으면_생성된다() {
            //when
            val restApiException = RestApiException(
                exceptionCode = COMMAND_ALREADY_PENDING,
                exceptionRecovery = ExceptionRecovery.queryCommand(TICKET_ID),
            )

            //then
            assertThat(restApiException.exceptionRecovery?.ticketId).isEqualTo(TICKET_ID)
        }

        @Test
        fun recovery가_필수인_code를_recovery_없이_만들면_실패한다() {
            //when & then
            assertThatThrownBy { RestApiException(COMMAND_ALREADY_PENDING) }
                .isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        fun 계약과_다른_kind의_recovery를_실으면_실패한다() {
            //when & then
            assertThatThrownBy {
                RestApiException(
                    exceptionCode = SESSION_RECOVERY_REQUIRED,
                    exceptionRecovery = ExceptionRecovery.refreshToday(),
                )
            }.isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        fun recovery가_금지된_code에_recovery를_실으면_실패한다() {
            //when & then
            assertThatThrownBy {
                RestApiException(
                    exceptionCode = ANSWER_NOT_FOUND,
                    exceptionRecovery = ExceptionRecovery.refreshToday(),
                )
            }.isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Nested
    inner class retryAfterSeconds를_검사할_때 {

        @Test
        fun RATE_LIMITED와_MAINTENANCE에는_실을_수_있다() {
            //when
            val rateLimited = RestApiException(
                exceptionCode = RATE_LIMITED,
                retryAfterSeconds = RETRY_AFTER_SECONDS,
            )
            val maintenance = RestApiException(
                exceptionCode = MAINTENANCE,
                retryAfterSeconds = RETRY_AFTER_SECONDS,
            )

            //then
            assertThat(rateLimited.retryAfterSeconds).isEqualTo(RETRY_AFTER_SECONDS)
            assertThat(maintenance.retryAfterSeconds).isEqualTo(RETRY_AFTER_SECONDS)
        }

        @Test
        fun 그_밖의_code에_실으면_실패한다() {
            //when & then
            assertThatThrownBy {
                RestApiException(
                    exceptionCode = INTERNAL_ERROR,
                    retryAfterSeconds = RETRY_AFTER_SECONDS,
                )
            }.isInstanceOf(IllegalStateException::class.java)
        }
    }

    companion object {
        private const val TICKET_ID = "ticket-1"
        private const val RETRY_AFTER_SECONDS = 30
    }
}
