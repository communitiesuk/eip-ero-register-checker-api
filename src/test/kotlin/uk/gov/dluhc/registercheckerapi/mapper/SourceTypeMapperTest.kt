package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckSourceType
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType as SourceTypeEntityEnum
import uk.gov.dluhc.registercheckerapi.dto.SourceType as SourceTypeDtoEnum
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckSourceType as SourceTypeSqsEnum
class SourceTypeMapperTest {
    private val mapper = SourceTypeMapperImpl()

    @ParameterizedTest
    @CsvSource(
        value = [
            "VOTER_MINUS_CARD, VOTER_CARD",
        ]
    )
    fun `should map Message Source Type enum to DTO Source Type`(
        sourceType: RegisterCheckSourceType,
        expected: SourceTypeDtoEnum
    ) {
        // Given

        // When
        val actual = mapper.fromSqsToDtoEnum(sourceType)

        // Then
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "VOTER_CARD, VOTER_MINUS_CARD",
        ]
    )
    fun `should map Entity Source Type to Message Source Type`(
        sourceType: SourceTypeEntityEnum,
        expected: SourceTypeSqsEnum
    ) {
        // Given

        // When
        val actual = mapper.fromEntityToSqsEnum(sourceType)

        // Then
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "VOTER_CARD, VOTER_CARD",
        ]
    )
    fun `should map Entity Source Type enum to DTO Source Type`(
        sourceType: SourceTypeEntityEnum,
        expected: SourceTypeDtoEnum
    ) {
        // Given

        // When
        val actual = mapper.fromEntityToDtoEnum(sourceType)

        // Then
        Assertions.assertThat(actual).isEqualTo(expected)
    }
}
