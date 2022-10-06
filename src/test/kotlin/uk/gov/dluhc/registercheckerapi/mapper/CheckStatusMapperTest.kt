package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckStatus

@ExtendWith(MockitoExtension::class)
internal class CheckStatusMapperTest {

    @InjectMocks
    private val mapper = CheckStatusMapperImpl()

    @Nested
    inner class ToCheckStatusEntityEnum {

        @ParameterizedTest
        @CsvSource(
            value = [
                "NO_MATCH, NO_MATCH",
                "EXACT_MATCH, EXACT_MATCH",
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
            val applicationStatusReason = mapper.toCheckStatusEntityEnum(registerCheckStatus)

            // Then
            assertThat(applicationStatusReason).isEqualTo(expected)
        }
    }
}
