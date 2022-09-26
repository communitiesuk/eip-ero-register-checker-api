package uk.gov.dluhc.registercheckerapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.PENDING
import uk.gov.dluhc.registercheckerapi.models.PendingRegisterChecksResponse
import uk.gov.dluhc.registercheckerapi.testsupport.assertj.PendingRegisterCheckAssert
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import java.util.UUID

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
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
    }

    @Test
    fun `should return ok with empty pending register check records given valid header key is present`() {
        // Given
        val eroIdFromIerApi = "camden-city-council"
        val gssCodeFromEroApi = getRandomGssCode()

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, gssCodeFromEroApi)

        // When
        val response = webTestClient.get()
            .uri(GET_PENDING_REGISTER_CHECKS_ENDPOINT)
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().isOk
            .returnResult(PendingRegisterChecksResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual!!.pageSize).isZero
        PendingRegisterCheckAssert.assertThat(actual.registerCheckRequests).hasEmptyPendingRegisterChecks()
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()
    }

    @Test
    fun `should return ok with multiple pending register check records in asc order`() {
        // Given
        val eroIdFromIerApi = "camden-city-council"
        val firstGssCodeFromEroApi = "E12345678"
        val secondGssCodeFromEroApi = "E98764532"
        val expectedRecordCount = 3

        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, eroIdFromIerApi)
        wireMockService.stubEroManagementGetEro(eroIdFromIerApi, firstGssCodeFromEroApi, secondGssCodeFromEroApi)

        val correlationId1 = UUID.fromString("14f66386-a86e-4dbc-af52-3327834f33d1")
        val correlationId2 = UUID.fromString("293dbc1d-81df-4db3-8ed6-64f05c083372")
        val correlationId3 = UUID.fromString("39147305-ba6d-4c14-8609-5d777afe4dc3")

        registerCheckRepository.save(buildRegisterCheck(correlationId = UUID.randomUUID(), gssCode = "UNKNOWN1", status = PENDING))
        val pendingRegisterCheckResult1ForGssCode1 = registerCheckRepository.save(buildRegisterCheck(correlationId = correlationId1, gssCode = firstGssCodeFromEroApi, status = PENDING))
        Thread.sleep(1000) // To ensure records are created 1 sec apart

        val pendingRegisterCheckResult2ForGssCode2 = registerCheckRepository.save(buildRegisterCheck(correlationId = correlationId2, gssCode = secondGssCodeFromEroApi, status = PENDING))
        Thread.sleep(1000) // To ensure records are created 1 sec apart

        val pendingRegisterCheckResult3ForGssCode1 = registerCheckRepository.save(buildRegisterCheck(correlationId = correlationId3, gssCode = firstGssCodeFromEroApi, status = PENDING))
        registerCheckRepository.save(buildRegisterCheck(gssCode = "UNKNOWN2"))

        // When
        val response = webTestClient.get()
            .uri(GET_PENDING_REGISTER_CHECKS_ENDPOINT)
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().isOk
            .returnResult(PendingRegisterChecksResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual!!.pageSize).isEqualTo(expectedRecordCount)
        PendingRegisterCheckAssert
            .assertThat(actual.registerCheckRequests)
            .isNotNull
            .hasPendingRegisterChecksInOrder(
                listOf(
                    pendingRegisterCheckResult1ForGssCode1,
                    pendingRegisterCheckResult2ForGssCode2,
                    pendingRegisterCheckResult3ForGssCode1
                )
            )
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()
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
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
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
        wireMockService.verifyEroManagementGetEroIdentifierNeverCalled()
    }

    @Test
    fun `should return internal server error given ERO service throws 404`() {
        // Given
        wireMockService.stubIerApiGetEroIdentifier(CERT_SERIAL_NUMBER_VALUE, "camden-city-council")
        wireMockService.stubEroManagementGetEroThrowsNotFoundError()

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
        assertThat(actual).isEqualTo("Error retrieving GSS codes")
        wireMockService.verifyGetEroIdentifierCalledOnce()
        wireMockService.verifyEroManagementGetEroIdentifierCalledOnce()
    }
}
