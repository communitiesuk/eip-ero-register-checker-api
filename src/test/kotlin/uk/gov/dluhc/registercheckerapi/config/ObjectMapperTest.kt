package uk.gov.dluhc.registercheckerapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime

internal class ObjectMapperTest {

    private val objectMapper = JacksonConfiguration().objectMapper()

    @Test
    fun `should map times without timezone information to OffsetDateTime`() {

        // Given
        val dateTime = "2023-10-28T20:50:07"
        val input = "{\"offsetDateTime\": \"$dateTime\"}"

        // When
        val output = objectMapper.readValue(input, OffsetDateTimeHolder::class.java)

        // Then
        assertThat(output.offsetDateTime.toLocalDateTime()).isEqualTo(LocalDateTime.parse(dateTime))
    }

    data class OffsetDateTimeHolder(val offsetDateTime: OffsetDateTime)
}
