package uk.gov.dluhc.registercheckerapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest

private const val GET_ERO_ENDPOINT = "/registercheck"
private const val REQUEST_HEADER_NAME = "client-cert-serial"
private const val CERT_SERIAL_NUMBER_VALUE = "543219999"

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
        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE)
        val expectedEroId = "1234"

        // When
        val response = webTestClient.get()
            .uri(GET_ERO_ENDPOINT)
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().isOk
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo(expectedEroId)
        wireMockService.verifyGetEroIdentifierCalledOnce()
    }

    @Test
    fun `should return ok with eroId given eroId is cached from a previous call`() {
        // Given
        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE)

        val initialEroId = webTestClient.get()
            .uri(GET_ERO_ENDPOINT)
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().isOk
            .returnResult(String::class.java)
            .responseBody
            .blockFirst()

        // When
        val cachedEroId = webTestClient.get()
            .uri(GET_ERO_ENDPOINT)
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().isOk
            .returnResult(String::class.java)
            .responseBody
            .blockFirst()

        // Then
        assertThat(cachedEroId).isEqualTo(initialEroId)
        wireMockService.verifyGetEroIdentifierCalledOnce()
    }

    @Test
    fun `should return eroId for different certificate serial not present earlier in cache`() {
        // Given
        val firstRequestCertSerialNumber = "543219999"
        val secondRequestCertSerialNumber = "453554535"
        val expectedNumberOfCalls = 2
        wireMockService.stubIerApiGetEroIdentifier(firstRequestCertSerialNumber, "12345")
        wireMockService.stubIerApiGetEroIdentifier(secondRequestCertSerialNumber, "67890")
        val initialEroId = webTestClient.get()
            .uri(GET_ERO_ENDPOINT)
            .header(REQUEST_HEADER_NAME, firstRequestCertSerialNumber)
            .exchange()
            .expectStatus().isOk
            .returnResult(String::class.java)
            .responseBody
            .blockFirst()

        // When
        val secondEroId = webTestClient.get()
            .uri(GET_ERO_ENDPOINT)
            .header(REQUEST_HEADER_NAME, secondRequestCertSerialNumber)
            .exchange()
            .expectStatus().isOk
            .returnResult(String::class.java)
            .responseBody
            .blockFirst()

        // Then
        assertThat(secondEroId).isNotEqualTo(initialEroId)
        wireMockService.verifyGetEroIdentifierCalled(expectedNumberOfCalls)
    }

    @Test
    fun `should return not found error given IER service throws 404`() {
        // Given
        wireMockService.stubIerApiGetEroIdentifierThrowsNotFoundError()

        // When
        val response = webTestClient.get()
            .uri(GET_ERO_ENDPOINT)
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().is4xxClientError
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo("EroId for certificate serial not found")
    }

    @Test
    fun `should not cache given IER service throws 404`() {
        // Given
        val expectedNumberOfCalls = 2
        wireMockService.stubIerApiGetEroIdentifierThrowsNotFoundError()
        val initialErrorResponse = webTestClient.get()
            .uri(GET_ERO_ENDPOINT)
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().is4xxClientError
            .returnResult(String::class.java)
            .responseBody
            .blockFirst()

        // When
        val secondErrorResponse = webTestClient.get()
            .uri(GET_ERO_ENDPOINT)
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().is4xxClientError
            .returnResult(String::class.java)
            .responseBody
            .blockFirst()

        // Then
        wireMockService.verifyGetEroIdentifierCalled(expectedNumberOfCalls)
    }

    @Test
    fun `should return internal server error given IER service throws 500`() {
        // Given
        wireMockService.stubIerApiGetEroIdentifierThrowsInternalServerError()

        // When
        val response = webTestClient.get()
            .uri(GET_ERO_ENDPOINT)
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().is5xxServerError
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo("Error getting eroId for certificate serial")
    }
}
