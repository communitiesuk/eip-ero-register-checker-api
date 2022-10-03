package uk.gov.dluhc.registercheckerapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
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
}