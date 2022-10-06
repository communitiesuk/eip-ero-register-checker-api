package uk.gov.dluhc.registercheckerapi.database.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckResultData
import java.util.UUID

internal class RegisterCheckResultDataRepositoryTest : IntegrationTest() {

    @Test
    fun `should save request body data`() {
        // Given
        val correlationId = UUID.randomUUID()
        val requestBodyJson = requestBodyJson(correlationId)
        val registerCheckResultData = RegisterCheckResultData(correlationId = correlationId, requestBody = requestBodyJson)
        registerCheckResultDataRepository.save(registerCheckResultData)

        // When
        val actual = registerCheckResultDataRepository.findByCorrelationId(correlationId)

        // Then
        assertThat(actual).isNotNull
        assertThat(actual?.id).isNotNull
        assertThat(actual?.correlationId).isEqualTo(correlationId)
        assertThat(actual?.requestBody?.contains(correlationId.toString())).isTrue
        assertThat(actual?.dateCreated).isNotNull
    }

    private fun requestBodyJson(requestId: UUID): String =
        """
        {
          "requestid": "$requestId",
          "gssCode": "T12345679",
          "createdAt": "2022-10-05T10:28:37.3052627+01:00",
          "registerCheckMatches": [],
          "registerCheckMatchCount": 0
        }
        """.trimIndent()
}
