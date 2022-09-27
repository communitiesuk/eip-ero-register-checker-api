package uk.gov.dluhc.registercheckerapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckResultRequest
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.models.buildRegisterCheckResultRequest
import java.util.UUID

internal class UpdatePendingRegisterCheckIntegrationTest : IntegrationTest() {

    companion object {
        private const val REQUEST_HEADER_NAME = "client-cert-serial"
        private const val CERT_SERIAL_NUMBER_VALUE = "543212222"
    }

    @Test
    fun `should return forbidden given valid header key is not present`() {
        webTestClient.post()
            .uri(buildUri())
            .body(
                Mono.just(buildRegisterCheckResultRequest()),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus()
            .isForbidden

        // Then
        wireMockService.verifyGetEroIdentifierCalled(0)
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
    }

    @Test
    fun `should return not found error given IER service throws 404`() {
        // Given
        wireMockService.stubIerApiGetEroIdentifierThrowsNotFoundError(certificateSerial = CERT_SERIAL_NUMBER_VALUE)

        // When
        val response = webTestClient.post()
            .uri(buildUri())
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .body(
                Mono.just(buildRegisterCheckResultRequest()),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus().is4xxClientError
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo("EROCertificateMapping for certificateSerial=[543219999] not found")
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
    }

    @Test
    fun `should return forbidden given valid header key is present but gssCode in requestBody does not matches gssCode from ERO`() {
        // Given
        val eroIdFromIerApi = "camden-city-council"
        val firstGssCodeFromEroApi = "E12345678"
        val secondGssCodeFromEroApi = "E98764532"
        val requestBodyGssCode = "E10101010"

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, firstGssCodeFromEroApi, secondGssCodeFromEroApi)

        webTestClient.post()
            .uri(buildUri())
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .body(
                Mono.just(buildRegisterCheckResultRequest(gssCode = requestBodyGssCode)),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus()
            .isForbidden

        // Then
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()
    }

    @Test
    fun `should return internal server error given IER service throws 500`() {
        // Given
        wireMockService.stubIerApiGetEroIdentifierThrowsInternalServerError(certificateSerial = CERT_SERIAL_NUMBER_VALUE)

        // When
        val response = webTestClient.post()
            .uri(buildUri())
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .body(
                Mono.just(buildRegisterCheckResultRequest()),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus().is5xxServerError
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo("Error getting eroId for certificate serial")
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
    }

    @Test
    fun `should return internal server error given ERO service throws 404`() {
        // Given
        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, "camden-city-council")
        wireMockService.stubEroManagementGetEroThrowsNotFoundError()

        // When
        val response = webTestClient.post()
            .uri(buildUri())
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .body(
                Mono.just(buildRegisterCheckResultRequest()),
                RegisterCheckResultRequest::class.java
            )
            .exchange()
            .expectStatus().is5xxServerError
            .returnResult(String::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo("Error retrieving GSS codes")
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()
    }

    private fun buildUri(requestId: String = UUID.randomUUID().toString()) =
        "/registerchecks/$requestId"
}
