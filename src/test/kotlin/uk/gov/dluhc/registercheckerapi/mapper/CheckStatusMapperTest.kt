package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.dluhc.applicationsapi.messaging.models.RegisterCheckResult
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus

internal class CheckStatusMapperTest {

    private val mapper = CheckStatusMapperImpl()

    @Nested
    inner class ToCheckStatusEntityEnum {

        @ParameterizedTest
        @CsvSource(
            value = [
                "NO_MATCH, NO_MATCH",
                "EXACT_MATCH, EXACT_MATCH",
                "PARTIAL_MATCH, PARTIAL_MATCH",
                "PENDING_DETERMINATION, PENDING_DETERMINATION",
                "EXPIRED, EXPIRED",
                "NOT_STARTED, NOT_STARTED",
                "MULTIPLE_MATCH, MULTIPLE_MATCH",
                "TOO_MANY_MATCHES, TOO_MANY_MATCHES"
            ]
        )
        fun `should map DTO RegisterCheckStatus enum to entity CheckStatus enum`(
            registerCheckStatus: RegisterCheckStatus,
            expected: CheckStatus
        ) {
            // Given

            // When
            val actual = mapper.toCheckStatusEntityEnum(registerCheckStatus)

            // Then
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    inner class ToRegisterCheckResultEnum {

        @ParameterizedTest
        @CsvSource(
            value = [
                "NO_MATCH, NO_MINUS_MATCH",
                "EXACT_MATCH, EXACT_MINUS_MATCH",
                "PARTIAL_MATCH, PARTIAL_MINUS_MATCH",
                "PENDING_DETERMINATION, PENDING_MINUS_DETERMINATION",
                "EXPIRED, EXPIRED",
                "NOT_STARTED, NOT_MINUS_STARTED",
                "MULTIPLE_MATCH, MULTIPLE_MINUS_MATCH",
                "TOO_MANY_MATCHES, TOO_MINUS_MANY_MINUS_MATCHES"
            ]
        )
        fun `should map entity CheckStatus enum to VCA RegisterCheckResult message enum`(
            checkStatus: CheckStatus,
            expected: RegisterCheckResult
        ) {
            // Given

            // When
            val actual = mapper.toRegisterCheckResultEnum(checkStatus)

            // Then
            assertThat(actual).isEqualTo(expected)
        }

        @ParameterizedTest
        @EnumSource(value = CheckStatus::class, names = ["PENDING"])
        fun `should fail to map entity enum given unsupported entity enum value`(source: CheckStatus) {
            // Given

            // When
            val ex = Assertions.catchThrowableOfType(
                { mapper.toRegisterCheckResultEnum(source) },
                IllegalArgumentException::class.java
            )

            // Then
            assertThat(ex).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(ex.message).isEqualTo("Unexpected enum constant: $source")
        }
    }
}
