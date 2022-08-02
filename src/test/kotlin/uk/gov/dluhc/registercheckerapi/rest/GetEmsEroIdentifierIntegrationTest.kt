package uk.gov.dluhc.registercheckerapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest

private const val GET_ERO_ENDPOINT = "/registercheck"
private const val REQUEST_HEADER_NAME = "client-cert-serial"

internal class GetEmsEroIdentifierIntegrationTest : IntegrationTest() {

    @Test
    fun `should return forbidden given valid header key is not present`() {
        webTestClient.get()
            .uri(GET_ERO_ENDPOINT)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return ok with eroId given valid header key is present`() {
        // Given
        val certSerialNumberValue = "543219999"
        wireMockService.stubIerApiGetEroIdentifier()

        val expectedEroId = "1234"

        // When
        val response = webTestClient.get()
            .uri(GET_ERO_ENDPOINT)
            .header(REQUEST_HEADER_NAME, certSerialNumberValue)
            .exchange()
            .expectStatus().isOk
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo(expectedEroId)
    }

    @Test
    fun `should return not found error given IER service throws 404`() {
        // Given
        val certSerialNumberValue = "543219888"
        wireMockService.stubIerApiGetEroIdentifierThrowsNotFoundError()

        // When
        val response = webTestClient.get()
            .uri(GET_ERO_ENDPOINT)
            .header(REQUEST_HEADER_NAME, certSerialNumberValue)
            .exchange()
            .expectStatus().is4xxClientError
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo("EroId for certificate serial not found")
    }

    @Test
    fun `should return internal server error given IER service throws 500`() {
        // Given
        val certSerialNumberValue = "543219252"
        wireMockService.stubIerApiGetEroIdentifierThrowsInternalServerError()

        // When
        val response = webTestClient.get()
            .uri(GET_ERO_ENDPOINT)
            .header(REQUEST_HEADER_NAME, certSerialNumberValue)
            .exchange()
            .expectStatus().is5xxServerError
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo("Error getting eroId for certificate serial")
    }
}
