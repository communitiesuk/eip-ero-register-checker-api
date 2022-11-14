package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.OffsetDateTime
import java.time.ZoneOffset

class InstantMapperTest {

    private val instantMapper = InstantMapper()

    @Test
    fun `should convert to UTC OffsetDateTime`() {
        // Given
        val instant = Instant.now()
        val expected = instant.atOffset(ZoneOffset.UTC)

        // When
        val actual = instantMapper.toOffsetDateTime(instant)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should convert OffsetDateTime to Instant`() {
        // Given
        val offsetDateTime = OffsetDateTime.now()
        val expected = offsetDateTime.toInstant()

        // When
        val actual = instantMapper.toInstant(offsetDateTime)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "2022-09-13T21:03:03.7788394+05:30, 2022-09-13T21:03:03.7788394+05:30",
            "1986-01-01T02:42:44.348Z, 1986-01-01T02:42:44.348Z",
            "1966-12-01T02:42:44.348+01:00, 1966-12-01T02:42:44.348+01:00",
            "1966-05-01T02:42:44.348+01:00, 1966-05-01T01:42:44.348Z",
            "1980-01-01T02:42:44.348+01:00, 1980-01-01T01:42:44.348Z",
            "2039-09-13T21:03:03.7788394+01:00, 2039-09-13T21:03:03.7788394+01:00",
        ]
    )
    fun `should convert OffsetDateTime string to Instant`(
        offsetDateTimeStr: String,
        expectedInstantStr: String
    ) {
        // Given
        val offsetDateTime = OffsetDateTime.parse(offsetDateTimeStr)
        val expected = Instant.parse(expectedInstantStr)

        // When
        val actual = instantMapper.toInstant(offsetDateTime)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should convert null OffsetDateTime to null Instant`() {
        // Given
        val offsetDateTime = null

        // When
        val actual = instantMapper.toInstant(offsetDateTime)

        // Then
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "2022, JULY,    1, 2022-07-01T00:00:00.000Z",
            "1986, JANUARY, 9, 1986-01-09T00:00:00.000Z"
        ]
    )
    fun `should convert LocalDate to Instant`(year: Int, month: Month, dayOfMonth: Int, expectedDateTimeStr: String) {
        // Given
        val inputLocalDate: LocalDate? = LocalDate.of(year, month, dayOfMonth)
        val expectedInstant = OffsetDateTime.parse(expectedDateTimeStr).toInstant()

        // When
        val actual = instantMapper.fromLocalDateToInstant(inputLocalDate)

        // Then
        assertThat(actual).isEqualTo(expectedInstant)
    }

    @Test
    fun `should convert null LocalDate to null Instant`() {
        // Given
        val inputLocalDate: LocalDate? = null
        val expectedInstant: Instant? = null

        // When
        val actual = instantMapper.fromLocalDateToInstant(inputLocalDate)

        // Then
        assertThat(actual).isEqualTo(expectedInstant)
    }
}
