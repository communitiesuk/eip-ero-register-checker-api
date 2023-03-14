package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.registercheckerapi.models.SourceSystem
import uk.gov.dluhc.registercheckerapi.database.entity.SourceType as SourceTypeEntityEnum
import uk.gov.dluhc.registercheckerapi.dto.SourceType as SourceTypeDtoEnum
import uk.gov.dluhc.registercheckerapi.messaging.models.SourceType as SourceTypeSqsEnum

class SourceTypeMapperTest {
    private val mapper = SourceTypeMapperImpl()

    @ParameterizedTest
    @CsvSource(
        value = [
            "VOTER_MINUS_CARD, VOTER_CARD",
            "POSTAL_MINUS_VOTE, POSTAL_VOTE",
            "PROXY_MINUS_VOTE, PROXY_VOTE",
            "OVERSEAS_MINUS_VOTE, OVERSEAS_VOTE",
        ]
    )
    fun `should map Message Source Type enum to DTO Source Type`(
        sourceType: SourceTypeSqsEnum,
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
            "POSTAL_VOTE, POSTAL_MINUS_VOTE",
            "PROXY_VOTE, PROXY_MINUS_VOTE",
            "OVERSEAS_VOTE, OVERSEAS_MINUS_VOTE",
        ]
    )
    fun `should map Entity Source Type to VCA Message Source Type`(
        sourceType: SourceTypeEntityEnum,
        expected: SourceTypeSqsEnum
    ) {
        // Given

        // When
        val actual = mapper.fromEntityToVcaSqsEnum(sourceType)

        // Then
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "VOTER_CARD, VOTER_CARD",
            "POSTAL_VOTE, POSTAL_VOTE",
            "PROXY_VOTE, PROXY_VOTE",
            "OVERSEAS_VOTE, OVERSEAS_VOTE",
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

    @ParameterizedTest
    @CsvSource(
        value = [
            "VOTER_CARD, VOTER_CARD",
            "POSTAL_VOTE, POSTAL_VOTE",
            "PROXY_VOTE, PROXY_VOTE",
            "OVERSEAS_VOTE, OVERSEAS_VOTE",
        ]
    )
    fun `should map DTO Source Type enum to Entity Source Type`(
        sourceType: SourceTypeDtoEnum,
        expected: SourceTypeEntityEnum
    ) {
        // Given

        // When
        val actual = mapper.fromDtoToEntityEnum(sourceType)

        // Then
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "VOTER_CARD, EROP",
            "POSTAL_VOTE, EROP",
            "PROXY_VOTE, EROP",
            "OVERSEAS_VOTE, EROP",
        ]
    )
    fun `should map DTO Source Type enum to Source system`(
        sourceType: SourceTypeDtoEnum,
        expected: SourceSystem
    ) {
        // Given

        // When
        val actual = mapper.sourceTypeDtoToSourceSystem(sourceType)

        // Then
        Assertions.assertThat(actual).isEqualTo(expected)
    }
}
