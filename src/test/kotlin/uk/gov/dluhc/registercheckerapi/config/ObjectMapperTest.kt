package uk.gov.dluhc.registercheckerapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime

internal class ObjectMapperTest {

    private val objectMapper = JacksonConfiguration().objectMapper()

    @Test
    fun `should map times without offset information to OffsetDateTime`() {

        // Given
        val dateTime = "2023-10-28T20:50:07"
        val input = "{\"offsetDateTime\": \"$dateTime\"}"

        // When
        val output = objectMapper.readValue(input, OffsetDateTimeHolder::class.java)

        // Then
        assertThat(output.offsetDateTime.toLocalDateTime()).isEqualTo(LocalDateTime.parse(dateTime))
    }

    @Test
    fun `should map times with default offset information to OffsetDateTime`() {

        // Given
        val dateTime = "2023-10-28T20:50:07Z"
        val input = "{\"offsetDateTime\": \"$dateTime\"}"

        // When
        val output = objectMapper.readValue(input, OffsetDateTimeHolder::class.java)

        // Then
        assertThat(output.offsetDateTime).isEqualTo(OffsetDateTime.parse(dateTime))
    }

    @Test
    fun `should map times with explicit offset information to OffsetDateTime`() {

        // Given
        val dateTime = "2023-10-28T21:50:07+01:00"
        val input = "{\"offsetDateTime\": \"$dateTime\"}"

        // When
        val output = objectMapper.readValue(input, OffsetDateTimeHolder::class.java)

        // Then
        assertThat(output.offsetDateTime).isEqualTo(OffsetDateTime.parse(dateTime))
    }

    data class OffsetDateTimeHolder(val offsetDateTime: OffsetDateTime)
}
