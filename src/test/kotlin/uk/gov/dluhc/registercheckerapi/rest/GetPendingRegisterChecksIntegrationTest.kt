package uk.gov.dluhc.registercheckerapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest

internal class GetPendingRegisterChecksIntegrationTest : IntegrationTest() {

    companion object {
        private const val GET_PENDING_REGISTER_CHECKS_ENDPOINT = "/registerchecks"
        private const val REQUEST_HEADER_NAME = "client-cert-serial"
        private const val CERT_SERIAL_NUMBER_VALUE = "543219999"
    }

    @Test
    fun `should return forbidden given valid header key is not present`() {
        webTestClient.get()
            .uri(GET_PENDING_REGISTER_CHECKS_ENDPOINT)
            .exchange()
            .expectStatus()
            .isForbidden
        wireMockService.verifyGetEroIdentifierCalled(0)
    }

    @Test
    fun `should return ok with eroId given valid header key is present`() {
        // Given
        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE)
        val expectedEroId = "1234"

        // When
        val response = webTestClient.get()
            .uri(GET_PENDING_REGISTER_CHECKS_ENDPOINT)
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
    fun `should return not found error given IER service throws 404`() {
        // Given
        wireMockService.stubIerApiGetEroIdentifierThrowsNotFoundError(certificateSerial = CERT_SERIAL_NUMBER_VALUE)

        // When
        val response = webTestClient.get()
            .uri(GET_PENDING_REGISTER_CHECKS_ENDPOINT)
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().is4xxClientError
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo("EROCertificateMapping for certificateSerial=[543219999] not found")
        wireMockService.verifyGetEroIdentifierCalledOnce()
    }

    @Test
    fun `should return internal server error given IER service throws 500`() {
        // Given
        wireMockService.stubIerApiGetEroIdentifierThrowsInternalServerError(certificateSerial = CERT_SERIAL_NUMBER_VALUE)

        // When
        val response = webTestClient.get()
            .uri(GET_PENDING_REGISTER_CHECKS_ENDPOINT)
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().is5xxServerError
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo("Error getting eroId for certificate serial")
        wireMockService.verifyGetEroIdentifierCalledOnce()
    }
}
