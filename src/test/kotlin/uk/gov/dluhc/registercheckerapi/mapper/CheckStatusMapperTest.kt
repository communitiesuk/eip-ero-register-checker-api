package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckResult

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
    inner class ToRegisterCheckStatusResultEnum {

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
        fun `should map entity CheckStatus enum to RegisterCheckResult message enum`(
            checkStatus: CheckStatus,
            expected: RegisterCheckResult
        ) {
            // Given

            // When
            val actual = mapper.toRegisterCheckStatusResultEnum(checkStatus)

            // Then
            assertThat(actual).isEqualTo(expected)
        }

        @ParameterizedTest
        @EnumSource(value = CheckStatus::class, names = ["PENDING"])
        fun `should fail to map entity enum given unsupported entity enum value`(source: CheckStatus) {
            // Given

            // When
            val ex = Assertions.catchThrowableOfType(
                { mapper.toRegisterCheckStatusResultEnum(source) },
                IllegalArgumentException::class.java
            )

            // Then
            assertThat(ex).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(ex.message).isEqualTo("Unexpected enum constant: $source")
        }
    }
}
