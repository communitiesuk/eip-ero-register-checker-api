package uk.gov.dluhc.registercheckerapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import uk.gov.dluhc.registercheckerapi.database.entity.CheckStatus.PENDING
import uk.gov.dluhc.registercheckerapi.models.ErrorResponse
import uk.gov.dluhc.registercheckerapi.models.PendingRegisterChecksResponse
import uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions.models.ErrorResponseAssert.Companion.assertThat
import uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions.models.PendingRegisterCheckAssert
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity.buildRegisterCheck
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class GetPendingRegisterChecksIntegrationTest : IntegrationTest() {

    companion object {
        private const val GET_PENDING_REGISTER_CHECKS_ENDPOINT = "/registerchecks"
        private const val QUERY_PARAM_PAGE_SIZE = "pageSize"
        const val REQUEST_HEADER_NAME = "client-cert-serial"
        const val CERT_SERIAL_NUMBER_VALUE = "543219999"
    }

    @Test
    fun `should return forbidden given valid header key is not present`() {
        webTestClient.get()
            .uri(buildUriStringWithQueryParam(10))
            .exchange()
            .expectStatus()
            .isForbidden
        wireMockService.verifyIerGetErosNeverCalled()
    }

    @Test
    fun `should return ok with empty pending register check records given valid header key is present and optional pageSize not present`() {
        // Given
        val eroId = "camden-city-council"
        val gssCode = getRandomGssCode()

        wireMockService.stubIerApiGetEros(CERT_SERIAL_NUMBER_VALUE, eroId, listOf(gssCode))

        // When
        val response = webTestClient.get()
            .uri(buildUriStringWithoutQueryParam())
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().isOk
            .returnResult(PendingRegisterChecksResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual!!.pageSize).isZero
        PendingRegisterCheckAssert.assertThat(actual.registerCheckRequests).hasEmptyPendingRegisterChecks()
        wireMockService.verifyIerGetErosCalledOnce()
    }

    @Test
    fun `should return ok with multiple pending register check records in asc order`() {
        // Given
        val eroId = "camden-city-council"
        val firstGssCode = "E12345678"
        val secondGssCode = "E98764532"
        val gssCodes = listOf(firstGssCode, secondGssCode)
        val expectedRecordCount = 3

        wireMockService.stubIerApiGetEros(CERT_SERIAL_NUMBER_VALUE, eroId, gssCodes)

        val correlationId1 = UUID.fromString("14f66386-a86e-4dbc-af52-3327834f33d1")
        val correlationId2 = UUID.fromString("293dbc1d-81df-4db3-8ed6-64f05c083372")
        val correlationId3 = UUID.fromString("39147305-ba6d-4c14-8609-5d777afe4dc3")

        registerCheckRepository.save(buildRegisterCheck(correlationId = UUID.randomUUID(), gssCode = "UNKNOWN1", status = PENDING))
        val pendingRegisterCheckResult1ForGssCode1 = registerCheckRepository.save(buildRegisterCheck(correlationId = correlationId1, gssCode = firstGssCode, status = PENDING))
        Thread.sleep(1000) // To ensure records are created 1 sec apart

        val pendingRegisterCheckResult2ForGssCode2 = registerCheckRepository.save(buildRegisterCheck(correlationId = correlationId2, gssCode = secondGssCode, status = PENDING))
        Thread.sleep(1000) // To ensure records are created 1 sec apart

        val pendingRegisterCheckResult3ForGssCode1 = registerCheckRepository.save(buildRegisterCheck(correlationId = correlationId3, gssCode = firstGssCode, status = PENDING))
        registerCheckRepository.save(buildRegisterCheck(gssCode = "UNKNOWN2"))

        // When
        val response = webTestClient.get()
            .uri(buildUriStringWithQueryParam(10))
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

        wireMockService.verifyIerGetErosCalledOnce()
    }

    @Test
    fun `should return not found error given IER service returns no ERO with a matching certificate serial number`() {
        // Given
        val eroId = "camden-city-council"
        val gssCode = getRandomGssCode()
        val certificateSerialNumberFromIerApi = "543218888"

        wireMockService.stubIerApiGetEros(certificateSerialNumberFromIerApi, eroId, listOf(gssCode))

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val response = webTestClient.get()
            .uri(buildUriStringWithQueryParam(10))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().is4xxClientError
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(404)
            .hasError("Not Found")
            .hasMessage("EROCertificateMapping for certificateSerial=[543219999] not found")
        wireMockService.verifyIerGetErosCalledOnce()
    }

    @Test
    fun `should return internal server error given IER service throws 500`() {
        // Given
        wireMockService.stubIerApiGetEroIdentifierThrowsInternalServerError()

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val response = webTestClient.get()
            .uri(buildUriStringWithQueryParam(10))
            .header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
            .exchange()
            .expectStatus().is5xxServerError
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(500)
            .hasError("Internal Server Error")
            .hasMessage("Error getting eroId for certificate serial")
        wireMockService.verifyIerGetErosCalledOnce()
    }

    private fun buildUriStringWithQueryParam(pageSize: Int) =
        UriComponentsBuilder
            .fromUriString(GET_PENDING_REGISTER_CHECKS_ENDPOINT)
            .queryParam(QUERY_PARAM_PAGE_SIZE, pageSize)
            .build().toUriString()

    private fun buildUriStringWithoutQueryParam() =
        UriComponentsBuilder
            .fromUriString(GET_PENDING_REGISTER_CHECKS_ENDPOINT)
            .build().toUriString()
}
