package uk.gov.dluhc.registercheckerapi.database.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckResultData
import java.util.UUID

internal class RegisterCheckRequestDataRepositoryTest : IntegrationTest() {

    @Test
    fun `should save request body data`() {
        // Given
        val correlationId = UUID.randomUUID()
        val requestBodyJson = requestBodyJson(correlationId)
        val registerCheckResultData = RegisterCheckResultData(correlationId = correlationId, requestBody = requestBodyJson)
        registerCheckRequestDataRepository.save(registerCheckResultData)

        // When
        val actual = registerCheckRequestDataRepository.findById(registerCheckResultData.id!!)

        // Then
        assertThat(actual.isPresent).isTrue
        with(actual.get()) {
            assertThat(id).isNotNull
            assertThat(this.correlationId).isEqualTo(correlationId)
            assertThat(requestBodyJson.contains(correlationId.toString())).isTrue
            assertThat(dateCreated).isNotNull
        }
    }

    private fun requestBodyJson(requestId: UUID): String =
        "{\n" +
            "\t\"requestid\": \"$requestId\",\n" +
            "\t\"gssCode\": \"T12345679\",\n" +
            "\t\"createdAt\": \"2022-10-05T10:28:37.3052627+01:00\",\n" +
            "\t\"registerCheckMatches\": [],\n" +
            "\t\"registerCheckMatchCount\": 0\n" +
            "}"
}
